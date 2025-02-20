# bedServer.py
from flask import Flask, request, jsonify
import pymysql
from datetime import datetime
import firebase_admin
from firebase_admin import credentials, messaging
import threading

# Firebase Admin 초기화
cred = credentials.Certificate("c:/Capstone/server/babybed-b6356-firebase-adminsdk-fbsvc-70560d5258.json")
firebase_admin.initialize_app(cred)

app = Flask(__name__)

def get_connection():
    return pymysql.connect(
        host='localhost',
        user='root',
        password='1234',
        db='babybed',
        charset='utf8'
    )

def print_bedinfo_on_startup():
    try:
        conn = get_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM BedInfo")
        rows = cursor.fetchall()
        print("==== [Startup] BedInfo Table Data ====")
        for row in rows:
            print(row)
        print("=======================================")
    except Exception as e:
        print("DB 조회 오류:", e)
    finally:
        cursor.close()
        conn.close()

# 전역 딕셔너리: 각 사용자(gdID)에 대해 알림 스케줄러 스레드 종료 이벤트 저장
notification_stop_events = {}

def run_notification_scheduler(user_id):
    try:
        from notification_scheduler import schedule_notifications_for_user
        stop_event = threading.Event()
        notification_stop_events[user_id] = stop_event
        schedule_notifications_for_user(user_id, stop_event)
    except Exception as e:
        print("Notification scheduler 실행 오류:", e)
    finally:
        if user_id in notification_stop_events:
            del notification_stop_events[user_id]

@app.route('/api/login', methods=['POST'])
def login():
    data = request.get_json()
    gdID = data.get('gdID')
    pw = data.get('password')
    auto_login = data.get('autoLogin', False)  # 클라이언트에서 autoLogin 플래그 전달

    if not gdID or not pw:
        return jsonify({"success": False, "message": "아이디와 비밀번호를 입력하세요."}), 400

    try:
        conn = get_connection()
        cursor = conn.cursor()
        sql = "SELECT JoinDate, FCM_toKen FROM GuardianLog WHERE GdID=%s AND pw=%s"
        cursor.execute(sql, (gdID, pw))
        result = cursor.fetchone()

        if result:
            join_date, fcm_token = result
            # autoLogin이 true이면 알림 스케줄러를 시작
            if auto_login:
                if gdID not in notification_stop_events:
                    threading.Thread(target=run_notification_scheduler, args=(gdID,), daemon=True).start()
            return jsonify({
                "success": True,
                "message": "로그인 성공",
                "gdID": gdID,
                "joinDate": str(join_date),
                "FCM_toKen": fcm_token
            }), 200
        else:
            return jsonify({"success": False, "message": "아이디 또는 비밀번호가 틀립니다."}), 401

    except Exception as e:
        print(e)
        return jsonify({"success": False, "message": "서버 오류: " + str(e)}), 500
    finally:
        cursor.close()
        conn.close()

@app.route('/api/register', methods=['POST'])
def register():
    data = request.get_json()
    gdID = data.get('gdID')
    password = data.get('password')
    fcm_token = data.get('fcmToken')

    if not gdID or not password or not fcm_token:
        return jsonify({"success": False, "message": "필수 입력값 누락"}), 400

    join_date_str = datetime.now().strftime('%Y-%m-%d')

    try:
        conn = get_connection()
        cursor = conn.cursor()
        sql_check = "SELECT GdID FROM GuardianLog WHERE GdID=%s"
        cursor.execute(sql_check, (gdID,))
        row = cursor.fetchone()
        if row:
            return jsonify({"success": False, "message": "이미 존재하는 아이디입니다."}), 409

        sql_insert = """
            INSERT INTO GuardianLog (GdID, pw, JoinDate, FCM_toKen)
            VALUES (%s, %s, %s, %s)
        """
        cursor.execute(sql_insert, (gdID, password, join_date_str, fcm_token))
        conn.commit()

        # 회원가입 후 환영 알림 전송 (선택 사항)
        send_registration_congrats(fcm_token, gdID)

        return jsonify({"success": True, "message": "회원가입 성공"}), 200

    except Exception as e:
        print("회원가입 오류:", e)
        return jsonify({"success": False, "message": str(e)}), 500
    finally:
        cursor.close()
        conn.close()

def send_registration_congrats(token, userId):
    message = messaging.Message(
        notification=messaging.Notification(
            title="회원가입 축하드립니다!",
            body=f"{userId}님, 가입을 환영합니다!"
        ),
        token=token
    )
    try:
        response = messaging.send(message)
        print("푸시 전송 성공:", response)
    except Exception as e:
        print("푸시 전송 실패:", e)


@app.route('/api/checkDuplicate', methods=['POST'])
def check_duplicate():
    data = request.get_json()
    gdID = data.get('gdID')
    if not gdID:
        return jsonify({"success": False, "message": "아이디가 누락되었습니다."}), 400
    try:
        conn = get_connection()
        cursor = conn.cursor()
        sql = "SELECT GdID FROM GuardianLog WHERE GdID = %s"
        cursor.execute(sql, (gdID,))
        result = cursor.fetchone()
        if result:
            # 아이디가 이미 존재하면 사용 불가능 (409 Conflict)
            return jsonify({"success": False, "available": False, "message": "이미 존재하는 아이디입니다."}), 409
        else:
            # 아이디가 없으면 사용 가능 (200 OK)
            return jsonify({"success": True, "available": True, "message": "사용 가능한 아이디입니다."}), 200
    except Exception as e:
        return jsonify({"success": False, "message": "서버 오류: " + str(e)}), 500
    finally:
        cursor.close()
        conn.close()



@app.route('/api/logout', methods=['POST'])
def logout():
    data = request.get_json()
    gdID = data.get('gdID')
    if not gdID:
        return jsonify({"success": False, "message": "gdID 누락"}), 400

    if gdID in notification_stop_events:
        notification_stop_events[gdID].set()
        return jsonify({"success": True, "message": f"{gdID} 로그아웃 및 알림 전송 중지"}), 200
    else:
        return jsonify({"success": False, "message": f"{gdID}에 대해 실행 중인 알림 스케줄러가 없습니다."}), 400

if __name__ == '__main__':
    print_bedinfo_on_startup()
    app.run(host='0.0.0.0', port=5000, debug=True)
