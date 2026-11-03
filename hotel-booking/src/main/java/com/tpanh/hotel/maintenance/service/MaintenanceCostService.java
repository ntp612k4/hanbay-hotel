package com.tpanh.hotel.maintenance.service;

import com.tpanh.hotel.common.exception.RequestValidationException;
import com.tpanh.hotel.common.security.CurrentUserProvider;
import com.tpanh.hotel.maintenance.domain.MaintenanceRequestStatus;
import com.tpanh.hotel.maintenance.dto.request.RecordMaintenanceCostRequest;
import com.tpanh.hotel.maintenance.dto.response.MaintenanceCostResponse;
import com.tpanh.hotel.maintenance.exception.MaintenanceRequestNotFoundException;
import com.tpanh.hotel.maintenance.repository.MaintenanceRequestRepository;
import java.sql.Date;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaintenanceCostService {
    private final MaintenanceRequestRepository requests;
    private final JdbcTemplate jdbc;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public MaintenanceCostResponse get(UUID id) {
        if (!requests.existsById(id)) throw new MaintenanceRequestNotFoundException();
        return read(id);
    }

    public List<MaintenanceCostResponse> history(UUID id) {
        if (!requests.existsById(id)) throw new MaintenanceRequestNotFoundException();
        return jdbc.query(
                "SELECT c.*, u.full_name FROM maintenance_cost_history c JOIN users u ON u.id=c.recorded_by WHERE request_id=? ORDER BY revision DESC",
                this::map,
                id);
    }

    @Transactional
    public MaintenanceCostResponse record(UUID id, RecordMaintenanceCostRequest r) {
        var request = requests.findByIdForUpdate(id).orElseThrow(MaintenanceRequestNotFoundException::new);
        if (request.getStatus() == MaintenanceRequestStatus.PENDING)
            throw new RequestValidationException("status", "Chỉ ghi chi phí khi đã bắt đầu bảo trì.");
        if (r == null || r.amount() == null || r.amount() < 0 || r.amount() > 999999999999L)
            throw new RequestValidationException("amount", "Số tiền không hợp lệ.");
        if (r.expenseDate() == null
                || r.expenseDate().isAfter(LocalDate.now(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh"))))
                || r.expenseDate()
                        .isBefore(request.getCreatedAt()
                                .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                                .toLocalDate()))
            throw new RequestValidationException("expenseDate", "Ngày chi phải từ ngày tạo yêu cầu đến hôm nay.");
        if (r.note() == null || r.note().isBlank() || r.note().trim().length() > 2000)
            throw new RequestValidationException(
                    "note", "Nhập nội dung chi phí hoặc lý do điều chỉnh (tối đa 2000 ký tự).");
        var existing = read(id);
        int revision = existing == null ? 0 : existing.revision();
        if (r.expectedRevision() == null || revision != r.expectedRevision())
            throw new MaintenanceCostConflictException();
        UUID author = currentUser
                .currentUserId()
                .orElseThrow(() ->
                        new org.springframework.security.access.AccessDeniedException("Không tìm thấy người ghi nhận"));
        var now = java.time.OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        jdbc.update(
                "INSERT INTO maintenance_costs(request_id,amount,expense_date,note,revision,recorded_by,recorded_at) VALUES (?,?,?,?,?,?,?) ON CONFLICT(request_id) DO UPDATE SET amount=EXCLUDED.amount,expense_date=EXCLUDED.expense_date,note=EXCLUDED.note,revision=EXCLUDED.revision,recorded_by=EXCLUDED.recorded_by,recorded_at=EXCLUDED.recorded_at",
                id,
                r.amount(),
                Date.valueOf(r.expenseDate()),
                r.note().trim(),
                revision + 1,
                author,
                now);
        jdbc.update(
                "INSERT INTO maintenance_cost_history(request_id,revision,amount,expense_date,note,recorded_by,recorded_at) SELECT request_id,revision,amount,expense_date,note,recorded_by,recorded_at FROM maintenance_costs WHERE request_id=?",
                id);
        return read(id);
    }

    private MaintenanceCostResponse read(UUID id) {
        return jdbc
                .query(
                        "SELECT c.*,u.full_name FROM maintenance_costs c JOIN users u ON u.id=c.recorded_by WHERE request_id=?",
                        this::map,
                        id)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private MaintenanceCostResponse map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new MaintenanceCostResponse(
                rs.getLong("amount"),
                rs.getObject("expense_date", LocalDate.class),
                rs.getString("note"),
                rs.getInt("revision"),
                rs.getObject("recorded_by", UUID.class),
                rs.getString("full_name"),
                rs.getTimestamp("recorded_at").toInstant());
    }
}
