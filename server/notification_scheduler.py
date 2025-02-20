# notification_scheduler.py
import time
import pymysql
from firebase_admin import messaging

def get_connection():
    return pymysql.connect(
        host='localhost',
        user='root',
        password='1234',
        db='babybed',
        charset='utf8'
    )

def schedule_notifications_for_user(user_id, stop_event):
    """
    주어진 사용자 아이디(user_id)를 기반으로 DB에서 FCM 토큰을 조회한 후,
    5초마다 해당 토큰으로 "요청이 왔습니다"라는 테스트 푸시 알림을 전송하며,
    stop_event가 설정되면 반복을 중지합니다.
    """
    while not stop_event.is_set():
        try:
            conn = get_connection()
            cursor = conn.cursor()
            sql = "SELECT FCM_toKen FROM GuardianLog WHERE GdID = %s"
            cursor.execute(sql, (user_id,))
            row = cursor.fetchone()
            if row:
                token = row[0]
                message = messaging.Message(
                    notification=messaging.Notification(
                        title="알림 도착",
                        body=f"{user_id}님, 요청이 왔습니다."
                    ),
                    token=token
                )
                try:
                    response = messaging.send(message)
                    print(f"[{user_id}] 푸시 전송 성공: {response}")
                except Exception as e:
                    print(f"[{user_id}] 푸시 전송 실패: {e}")
            else:
                print(f"[{user_id}] DB에서 토큰을 찾을 수 없습니다.")
        except Exception as e:
            print(f"[{user_id}] 알림 전송 중 오류 발생: {e}")
        finally:
            try:
                cursor.close()
                conn.close()
            except Exception:
                pass
        time.sleep(5)
    print(f"Notification scheduler for {user_id} stopped.")
