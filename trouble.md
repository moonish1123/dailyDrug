# Trouble Shooting / Action Items

## ✅ 완료된 항목

- [x] **월별 복용 기록 늘려두기**
  - `MedicationRepository.ensureOccurrencesUpTo`와 `DailyScheduleWorker`가 다음 달 말일까지 자동 보충하도록 구현 완료.
  - 매달 1일 자정에 자동으로 다음 달 말일까지 일정 생성.
  - 중복 방지 로직으로 안전하게 무기한 스케줄 지원.

- [x] **재알림 워커에 약 정보 전달하기**
  - `MedicationReminderWorker`가 DB에서 약 정보를 조회하여 복구하도록 구현 완료.
  - WorkData에는 `recordId`만 전달하지만, Worker 내부에서 `getScheduledDose()`로 전체 정보 조회.

- [x] **주기적 재알림 시스템**
  - 약 복용 안해도 1시간마다 자동으로 재알림 발송.
  - `MedicationAlarmReceiver.ACTION_REMIND`에서 알림 표시 후 자동으로 `scheduleRealert()` 호출.
  - 복용 완료 시 `cancelReminder()`로 재알림 중단.

## ✅ 추가 완료 항목

- [x] **알림 탭 시 바로 상세화면 열기**
  - 알림 콘텐츠 인텐트가 약 ID를 포함하도록 변경하고, `DailyDrugApp`이 해당 ID를 감지해 상세 화면으로 즉시 이동하도록 구현했습니다.

- [x] **재알림 워커 메타데이터 복구**
  - `MedicationReminderWorker`가 Repository에서 복용 정보를 조회해 재알림에 필요한 데이터를 전달하게 했습니다.

- [x] **위젯 갱신 보강**
  - 알림 스케줄링/취소 시 홈 위젯이 자동으로 새로고침되도록 `ReminderScheduler.notifyWidgets()`를 호출합니다.
