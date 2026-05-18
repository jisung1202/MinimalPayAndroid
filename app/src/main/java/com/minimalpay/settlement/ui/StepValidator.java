package com.minimalpay.settlement.ui;

/**
 * 위저드 단계별 진행 조건 검증.
 */
public final class StepValidator {

    public static final int STEP_GROUP = 0;
    public static final int STEP_EXPENSE = 1;
    public static final int STEP_REPORT = 2;
    public static final int STEP_TRANSFER = 3;
    public static final int STEP_COUNT = 4;

    private StepValidator() {
    }

    public static ValidationResult validate(SettlementSession session, int step) {
        return switch (step) {
            case STEP_GROUP -> validateGroup(session);
            case STEP_EXPENSE -> validateExpense(session);
            case STEP_REPORT -> validateReport(session);
            case STEP_TRANSFER -> ValidationResult.ok();
            default -> ValidationResult.fail("알 수 없는 단계입니다.");
        };
    }

    private static ValidationResult validateGroup(SettlementSession session) {
        if (!session.getController().hasGroup()) {
            return ValidationResult.fail("그룹명을 입력하고 [그룹 생성]을 눌러주세요.");
        }
        if (session.getGroupName() == null || session.getGroupName().isBlank()) {
            return ValidationResult.fail("그룹명을 입력해 주세요.");
        }
        int expected = session.getExpectedMemberCount();
        if (expected <= 0) {
            return ValidationResult.fail("참여 인원 수(명)를 입력해 주세요.");
        }
        int added = session.getMembers().size();
        if (added < expected) {
            return ValidationResult.fail(
                    String.format("멤버를 %d명 모두 추가해 주세요. (현재 %d/%d명)", expected, added, expected));
        }
        return ValidationResult.ok();
    }

    private static ValidationResult validateExpense(SettlementSession session) {
        if (!session.getController().hasGroup()) {
            return ValidationResult.fail("먼저 그룹 설정을 완료해 주세요.");
        }
        if (session.getController().getExpenseCount() < 1) {
            return ValidationResult.fail("지출을 1건 이상 등록한 뒤 [다음]을 눌러주세요.");
        }
        return ValidationResult.ok();
    }

    private static ValidationResult validateReport(SettlementSession session) {
        if (session.getLastReport() == null) {
            return ValidationResult.fail("[정산 리포트 생성] 버튼을 눌러 리포트를 만든 뒤 [다음]을 눌러주세요.");
        }
        return ValidationResult.ok();
    }

    public static final class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        static ValidationResult ok() {
            return new ValidationResult(true, "");
        }

        static ValidationResult fail(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
