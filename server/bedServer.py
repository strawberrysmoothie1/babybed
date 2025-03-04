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
        responseDict = {"success": False, "message": "아이디와 비밀번호를 입력하세요."}
        print("login response:", responseDict)
        return jsonify(responseDict), 400

    try:
        conn = get_connection()
        cursor = conn.cursor()
        sql = "SELECT JoinDate, FCM_toKen FROM GuardianLog WHERE GdID=%s AND pw=%s"
        cursor.execute(sql, (gdID, pw))
        result = cursor.fetchone()

        if result:
            join_date, fcm_token = result
            if auto_login:
                if gdID not in notification_stop_events:
                    threading.Thread(target=run_notification_scheduler, args=(gdID,), daemon=True).start()
            responseDict = {
                "success": True,
                "message": "로그인 성공",
                "gdID": gdID,
                "joinDate": str(join_date),
                "FCM_toKen": fcm_token
            }
            print("login response:", responseDict)
            return jsonify(responseDict), 200
        else:
            responseDict = {"success": False, "message": "아이디 또는 비밀번호가 틀립니다."}
            print("login response:", responseDict)
            return jsonify(responseDict), 401

    except Exception as e:
        print(e)
        responseDict = {"success": False, "message": "서버 오류: " + str(e)}
        print("login response:", responseDict)
        return jsonify(responseDict), 500
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
        responseDict = {"success": False, "message": "필수 입력값 누락"}
        print("register response:", responseDict)
        return jsonify(responseDict), 400

    join_date_str = datetime.now().strftime('%Y-%m-%d')

    try:
        conn = get_connection()
        cursor = conn.cursor()
        sql_check = "SELECT GdID FROM GuardianLog WHERE GdID=%s"
        cursor.execute(sql_check, (gdID,))
        row = cursor.fetchone()
        if row:
            responseDict = {"success": False, "message": "이미 존재하는 아이디입니다."}
            print("register response:", responseDict)
            return jsonify(responseDict), 409

        sql_insert = """
            INSERT INTO GuardianLog (GdID, pw, JoinDate, FCM_toKen)
            VALUES (%s, %s, %s, %s)
        """
        cursor.execute(sql_insert, (gdID, password, join_date_str, fcm_token))
        conn.commit()

        send_registration_congrats(fcm_token, gdID)

        responseDict = {"success": True, "message": "회원가입 성공"}
        print("register response:", responseDict)
        return jsonify(responseDict), 200

    except Exception as e:
        print("회원가입 오류:", e)
        responseDict = {"success": False, "message": str(e)}
        print("register response:", responseDict)
        return jsonify(responseDict), 500
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
        responseDict = {"success": False, "message": "아이디가 누락되었습니다."}
        print("checkDuplicate response:", responseDict)
        return jsonify(responseDict), 400
    try:
        conn = get_connection()
        cursor = conn.cursor()
        sql = "SELECT GdID FROM GuardianLog WHERE GdID = %s"
        cursor.execute(sql, (gdID,))
        result = cursor.fetchone()
        if result:
            responseDict = {"success": True, "available": False, "message": "이미 존재하는 아이디입니다."}
        else:
            responseDict = {"success": True, "available": True, "message": "사용 가능한 아이디입니다."}
        print("checkDuplicate response:", responseDict)
        return jsonify(responseDict), 200
    except Exception as e:
        responseDict = {"success": False, "message": "서버 오류: " + str(e)}
        print("checkDuplicate response:", responseDict)
        return jsonify(responseDict), 500
    finally:
        cursor.close()
        conn.close()

@app.route('/api/logout', methods=['POST'])
def logout():
    data = request.get_json()
    gdID = data.get('gdID')
    if not gdID:
        responseDict = {"success": False, "message": "gdID 누락"}
        print("logout response:", responseDict)
        return jsonify(responseDict), 400

    if gdID in notification_stop_events:
        notification_stop_events[gdID].set()
        responseDict = {"success": True, "message": f"{gdID} 로그아웃 및 알림 전송 중지"}
        print("logout response:", responseDict)
        return jsonify(responseDict), 200
    else:
        responseDict = {"success": False, "message": f"{gdID}에 대해 실행 중인 알림 스케줄러가 없습니다."}
        print("logout response:", responseDict)
        return jsonify(responseDict), 400

@app.route('/api/checkMyBed', methods=['POST'])
def check_my_bed():
    data = request.get_json()
    gdID = data.get('gdID')
    print("check_my_bed gdID:", gdID)
    if not gdID:
        responseDict = {"success": False, "message": "gdID 누락"}
        print("checkMyBed response:", responseDict)
        return jsonify(responseDict), 400
    try:
        conn = get_connection()
        cursor = conn.cursor()
        sql = """
            SELECT gb.GdID, gb.bedID, gb.designation, DATE_FORMAT(gb.period, '%%Y-%%m-%%d') as period, 
                CAST(gb.bed_order AS CHAR) as bed_order, bi.serialNumber
            FROM GuardBed gb
            JOIN BedInfo bi ON gb.bedID = bi.bedID
            WHERE gb.GdID = %s
            ORDER BY gb.bed_order ASC
        """
        print("Executing SQL:", sql, "with parameter:", gdID)
        cursor.execute(sql, (gdID,))
        rows = cursor.fetchall()
        print("Raw rows fetched:", rows)
        bedInfo = [list(row) for row in rows]
        for row in bedInfo:
            print("Row:", row)
        responseDict = {"success": True, "bedInfo": bedInfo}
        print("checkMyBed response:", responseDict)
        return jsonify(responseDict), 200
    except Exception as e:
        print("Exception in check_my_bed:", e)
        responseDict = {"success": False, "message": "서버 오류: " + str(e)}
        print("checkMyBed response:", responseDict)
        return jsonify(responseDict), 500
    finally:
        cursor.close()
        conn.close()
        

@app.route('/api/calcBedCounts', methods=['POST'])
def calc_bed_counts():
    # GdID 값은 무시하고 전체 데이터를 대상으로 계산함
    try:
        conn = get_connection()
        cursor = conn.cursor()
        sql = """
            SELECT gb.GdID, gb.bedID, gb.designation, DATE_FORMAT(gb.period, '%%Y-%%m-%%d') as period, 
                   CAST(gb.bed_order AS CHAR) as bed_order, bi.serialNumber
            FROM GuardBed gb
            JOIN BedInfo bi ON gb.bedID = bi.bedID
            ORDER BY gb.bed_order ASC
        """
        print("Executing SQL:", sql)
        cursor.execute(sql)
        rows = cursor.fetchall()
        print("Raw rows fetched for calc:", rows)
        
        # 그룹화: 같은 bedID별로 묶기
        groups = {}
        for row in rows:
            bedID = row[1]
            groups.setdefault(bedID, []).append(row)
        
        bedCounts = []
        for bedID, groupRows in groups.items():
            designation = groupRows[0][2]
            serialNumber = groupRows[0][5]
            guardianCount = 0
            tempCount = 0
            periodDisplay = ""
            remainingDays = 0
            for row in groupRows:
                period = row[3]
                # period가 None, 빈 문자열 또는 "null"(대소문자 무시)이면 보호자로 간주
                if period is None or period.strip() == "" or period.strip().lower() == "null":
                    guardianCount += 1
                else:
                    tempCount += 1
                    periodDisplay = period
                    try:
                        periodDate = datetime.strptime(period, "%Y-%m-%d").date()
                        today = datetime.today().date()
                        remainingDays = (periodDate - today).days
                    except Exception as e:
                        remainingDays = 0
            bedCount = {
                "bedID": bedID,
                "designation": designation,
                "guardianCount": guardianCount,
                "tempCount": tempCount,
                "serialNumber": serialNumber,
                "period": periodDisplay,
                "remainingDays": remainingDays
            }
            bedCounts.append(bedCount)
            # 터미널에 각 침대별 계산 결과 출력
            print("Calculated bed count:", bedCount)
            
        responseDict = {"success": True, "bedCounts": bedCounts}
        print("calcBedCounts response:", responseDict)
        return jsonify(responseDict), 200
    except Exception as e:
        print("Exception in calc_bed_counts:", e)
        responseDict = {"success": False, "message": "서버 오류: " + str(e)}
        print("calcBedCounts response:", responseDict)
        return jsonify(responseDict), 500
    finally:
        cursor.close()
        conn.close()



@app.route('/api/deleteBed', methods=['POST'])
def delete_bed():
    data = request.get_json()
    gdID = data.get('gdID')
    bedID = data.get('bedID')
    password = data.get('password')
    if not gdID or not bedID or not password:
        responseDict = {"success": False, "message": "필수 값 누락"}
        print("deleteBed response:", responseDict)
        return jsonify(responseDict), 400
    try:
        conn = get_connection()
        cursor = conn.cursor()
        # GuardianLog 테이블에서 비밀번호 조회
        sql_check = "SELECT pw FROM GuardianLog WHERE GdID = %s"
        cursor.execute(sql_check, (gdID,))
        row = cursor.fetchone()
        if not row:
            responseDict = {"success": False, "message": "존재하지 않는 사용자입니다."}
            print("deleteBed response:", responseDict)
            return jsonify(responseDict), 404
        db_pw = row[0]
        if db_pw != password:
            responseDict = {"success": False, "message": "비밀번호가 일치하지 않습니다."}
            print("deleteBed response:", responseDict)
            return jsonify(responseDict), 403
        
        # GuardBed 테이블에서 해당 튜플 삭제
        sql_delete = "DELETE FROM GuardBed WHERE GdID = %s AND bedID = %s"
        cursor.execute(sql_delete, (gdID, bedID))
        conn.commit()
        responseDict = {"success": True, "message": "삭제 성공"}
        print("deleteBed response:", responseDict)
        return jsonify(responseDict), 200
    except Exception as e:
        print("Exception in delete_bed:", e)
        responseDict = {"success": False, "message": "서버 오류: " + str(e)}
        print("deleteBed response:", responseDict)
        return jsonify(responseDict), 500
    finally:
        cursor.close()
        conn.close()

@app.route('/api/checkBedInfo', methods=['POST'])
def check_bed_info():
    data = request.get_json()
    serial = data.get('serialNumber')
    if not serial:
        responseDict = {"success": False, "message": "serialNumber 누락"}
        print("checkBedInfo response:", responseDict)
        return jsonify(responseDict), 400
    try:
        conn = get_connection()
        cursor = conn.cursor()
        sql = "SELECT bedID FROM BedInfo WHERE serialNumber = %s"
        cursor.execute(sql, (serial,))
        row = cursor.fetchone()
        if row:
            bedID = row[0]
            responseDict = {"success": True, "message": "침대 정보 확인", "bedID": bedID}
            print("checkBedInfo response:", responseDict)
            return jsonify(responseDict), 200
        else:
            responseDict = {"success": False, "message": "침대 정보가 없습니다."}
            print("checkBedInfo response:", responseDict)
            return jsonify(responseDict), 404
    except Exception as e:
        responseDict = {"success": False, "message": "서버 오류: " + str(e)}
        print("checkBedInfo response:", responseDict)
        return jsonify(responseDict), 500
    finally:
        cursor.close()
        conn.close()

@app.route('/api/addGuardBed', methods=['POST'])
def add_guard_bed():
    data = request.get_json()
    gdID = data.get('gdID')
    bedID = data.get('bedID')
    designation = data.get('designation')
    period = data.get('period')  # 빈 문자열이면 null로 처리
    if not gdID or not bedID or not designation:
        responseDict = {"success": False, "message": "필수 값 누락"}
        print("addGuardBed response:", responseDict)
        return jsonify(responseDict), 400
    try:
        conn = get_connection()
        cursor = conn.cursor()
        # bed_order: 해당 gdID의 GuardBed 레코드 수 + 1
        sql_count = "SELECT COUNT(*) FROM GuardBed WHERE GdID = %s"
        cursor.execute(sql_count, (gdID,))
        count_row = cursor.fetchone()
        if count_row:
            bed_order = count_row[0] + 1
        else:
            bed_order = 1
        sql_insert = """
            INSERT INTO GuardBed (GdID, bedID, designation, period, bed_order)
            VALUES (%s, %s, %s, %s, %s)
        """
        cursor.execute(sql_insert, (gdID, bedID, designation, period if period != "" else None, bed_order))
        conn.commit()
        responseDict = {"success": True, "message": "침대 추가 성공"}
        print("addGuardBed response:", responseDict)
        return jsonify(responseDict), 200
    except Exception as e:
        responseDict = {"success": False, "message": "서버 오류: " + str(e)}
        print("addGuardBed response:", responseDict)
        return jsonify(responseDict), 500
    finally:
        cursor.close()
        conn.close()





if __name__ == '__main__':
    print_bedinfo_on_startup()
    app.run(host='0.0.0.0', port=5000, debug=True)
