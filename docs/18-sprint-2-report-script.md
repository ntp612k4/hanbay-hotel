# Kịch bản Sprint 2 Report — Trần Phước Anh

> Vai trò trong buổi báo cáo: Team Leader và người phụ trách U023, U026, U031
>
> Sprint: 17/08/2026–31/08/2026
>
> Thời lượng đề xuất: 15–18 phút
> Phạm vi tài liệu: lời dẫn của leader, phần báo cáo cá nhân và thao tác demo. Thành viên tự chuẩn bị nội dung task của mình.

---

## 1. Mục tiêu Sprint và nguyên tắc demo

### Sprint Goal đề xuất

> Hoàn thiện luồng vận hành cốt lõi của khách sạn từ booking đã xác nhận đến nhận phòng, trả phòng và đưa phòng trở lại trạng thái sẵn sàng; đồng thời bổ sung công cụ AI hỗ trợ khách chọn phòng và các chức năng quản trị vận hành trong Sprint 2.

### Các Acceptance Criteria quan trọng cần chứng minh

1. Booking `CONFIRMED` đúng ngày nhận phòng chỉ được gán phòng vật lý `AVAILABLE` và đúng hạng phòng.
2. Check-in thành công cập nhật đồng thời booking thành `CHECKED_IN`, phòng thành `OCCUPIED` và lưu thời điểm nhận phòng.
3. Không cho check-in booking sai trạng thái, sai ngày, sai hạng phòng hoặc phòng không còn trống.
4. AI nhận yêu cầu lưu trú, sử dụng tình trạng phòng hiện tại để đề xuất hạng phòng phù hợp và cho phép khách tiếp tục sang bước đặt phòng.
5. AI chỉ hỗ trợ tư vấn, không tự tạo booking nếu khách chưa xác nhận trên giao diện.

### Tỷ lệ thời gian demo

| Nhóm nội dung | Thời lượng | Tỷ lệ |
|---|---:|---:|
| Happy path/core business flow | 11–13 phút | khoảng 75% |
| Alternative/unhappy path đáng chú ý | 3–4 phút | khoảng 25% |

---

## 2. Chuẩn bị trước buổi báo cáo

### Dữ liệu bắt buộc

Chuẩn bị và ghi ra giấy hoặc file note, không tìm ngẫu nhiên khi đang demo:

| Ký hiệu trong script | Dữ liệu cần chuẩn bị |
|---|---|
| Tài khoản Receptionist | `reception.demo@hanbay.local` — role `RECEPTIONIST` |
| Tài khoản Guest | `guest.demo@hanbay.local` — tài khoản active, không có staff role |
| Booking check-in chính | `BK-S2-CHECKIN-01` — `CONFIRMED`, 03/09/2026–05/09/2026, Deluxe, 2 người lớn + 1 trẻ em |
| Booking check-in dự phòng | `BK-S2-CHECKIN-02` — `CONFIRMED`, 03/09/2026–05/09/2026, Deluxe |
| Phòng check-in chính/dự phòng | `D-S2-01` và `D-S2-02` — `AVAILABLE`, Deluxe, tầng 9 |
| Booking không tồn tại | `BK-S2-NOT-FOUND` — mã cố định, không có trong database |
| Booking sai trạng thái | `BK-S2-PENDING` — `PENDING`, chưa thanh toán |
| Booking trả hôm nay | `BK-S2-CHECKOUT` — `CHECKED_IN`, phòng `S-S2-01`, đã thanh toán đủ |
| Khoảng ngày AI | `10/09/2026–13/09/2026` — 4 hạng phòng còn availability khi đã kiểm tra |

### Checklist kỹ thuật

- Mở sẵn frontend, backend, PostgreSQL và kiểm tra health/API trước giờ báo cáo.
- Đặt zoom trình duyệt 90–100%; đóng tab và thông báo không liên quan.
- Mở sẵn hai cửa sổ hoặc hai browser profile: một `RECEPTIONIST`, một `GUEST`.
- Không để lộ file `.env`, token, mật khẩu hoặc connection string trên màn hình.
- Chạy thử toàn bộ kịch bản một lần và reset dữ liệu. Một booking đã check-in không thể dùng lại cho happy path.
- Chụp sẵn ảnh dự phòng của: danh sách khách đến, danh sách phòng khả dụng, màn hình check-in thành công và câu trả lời AI.

### Quy tắc điều phối

- Leader mở đầu, công bố Sprint Goal và thứ tự báo cáo.
- Mỗi thành viên chỉ báo cáo task do chính mình phụ trách: kết quả, AC đã đạt, demo và khó khăn.
- Trước khi chuyển người, leader chốt một câu liên hệ kết quả vừa demo với Sprint Goal.
- Nếu demo lỗi kỹ thuật quá 30 giây, dùng ảnh/video dự phòng, nói rõ đây là lỗi môi trường và tiếp tục agenda.
- Leader giữ giờ và nhắc còn 1 phút nếu thành viên vượt thời lượng.

---

## 3. Timeline tổng thể cho leader

| Mốc | Người thực hiện | Nội dung | Thời lượng |
|---|---|---|---:|
| 00:00–01:30 | Trần Phước Anh | Mở đầu, Sprint Goal, phạm vi và trạng thái sprint | 1:30 |
| 01:30–02:00 | Trần Phước Anh | Giới thiệu thứ tự và mời từng member | 0:30 |
| 02:00–07:00 | Các member | Tự báo cáo theo ownership; leader dùng câu chuyển cụ thể ở mục 4 | 5:00 |
| 07:00–11:30 | Trần Phước Anh | U023 + U026: gán phòng và check-in | 4:30 |
| 11:30–14:30 | Trần Phước Anh | U031: AI room recommendation | 3:00 |
| 14:30–16:00 | Trần Phước Anh | Tổng kết, tồn đọng, bước tiếp theo | 1:30 |

Nếu buổi báo cáo chỉ có 15 phút, rút phần member xuống theo timebox được giao và giữ nguyên happy path U023/U026; chỉ demo một unhappy path.

---

## 4. Script mở đầu và điều phối

### 00:00–00:30 — Chào và giới thiệu

**Trên màn hình:** Mở slide/trang Sprint Overview có Sprint Goal và bảng user story. Chưa mở code.

**Nói:**

> Em xin chào thầy/cô và các bạn. Em là Trần Phước Anh, leader của nhóm HANBAY Hotel. Hôm nay em sẽ điều phối Sprint 2 Report, sau đó trực tiếp báo cáo các user story em phụ trách là U023, U026 và U031. Mỗi thành viên sẽ tự trình bày phần việc của mình để thể hiện rõ ownership và kết quả đã hoàn thành.

### 00:30–01:30 — Sprint Goal và trạng thái

**Trên màn hình:** Trỏ lần lượt vào Sprint Goal, cột `Done`, `InProgress`, `To-do`.

**Nói:**

> Sprint Goal của nhóm là hoàn thiện luồng vận hành cốt lõi từ booking đã xác nhận, nhận phòng, trả phòng đến housekeeping; đồng thời bổ sung AI hỗ trợ khách chọn phòng và các chức năng quản trị trong Sprint 2.
>
> Trong danh sách sprint, U034 đã được cập nhật thành Done. Hai hạng mục chưa hoàn tất cần nói rõ là U035 vẫn InProgress và U036 đang To-do. Vì vậy hôm nay nhóm chỉ xác nhận những Acceptance Criteria có bằng chứng chạy được; nhóm không coi các phần chưa kiểm chứng là đã hoàn tất.
>
> Phần demo ưu tiên happy path và core business flow. Các tình huống lỗi chỉ trình bày khi chúng làm rõ một business rule quan trọng.

### 01:30–02:00 — Mời thành viên đầu tiên

**Trên màn hình:** Trỏ vào U025, U029 và U033 trong bảng Sprint.

**Nói:**

> Thứ tự báo cáo sẽ đi từ luồng khách đến, vận hành khách sạn đến các chức năng quản trị và AI. Đầu tiên, em xin mời bạn **Lê Trần Hữu Mạnh** trình bày **U025, U029 và U033**: danh sách khách đến hôm nay, quản lý dịch vụ bổ sung và Manager Dashboard.

### Câu chuyển sau phần Lê Trần Hữu Mạnh

**Trên màn hình:** Khi bạn Mạnh kết thúc, trỏ lại ba task U025, U029, U033; sau đó chuyển con trỏ sang U021 và U027.

**Nói:**

> Cảm ơn bạn **Lê Trần Hữu Mạnh**. Phần vừa rồi đã chứng minh hệ thống lọc đúng booking `CONFIRMED` đến trong ngày, quản lý được danh mục add-on mà không làm thay đổi snapshot booking cũ, đồng thời hiển thị các chỉ số revenue, occupancy và booking cho Manager. Các kết quả này đóng góp vào hai mục tiêu của Sprint là chuẩn bị dữ liệu cho quy trình đón khách và cung cấp công cụ quản trị vận hành. Tiếp theo em mời bạn **Trần Nguyên** trình bày **U021 và U027**.

### Câu chuyển sau phần Trần Nguyên

**Trên màn hình:** Khi bạn Nguyên kết thúc, trỏ U021 và U027; sau đó chuyển con trỏ sang U024, U020, U022, U032, U037.

**Nói:**

> Cảm ơn bạn **Trần Nguyên**. Phần vừa rồi đã chứng minh booking `CHECKED_IN` và đã thanh toán đủ có thể được check-out, booking chuyển `CHECKED_OUT`, phòng chuyển `DIRTY`; đồng thời Manager có thể tạo, cập nhật và deactivate quy tắc giá cuối tuần hoặc ngày lễ. Các kết quả này đóng góp vào giai đoạn trả phòng và khả năng quản lý giá của khách sạn. Tiếp theo em mời bạn **Nguyễn Thanh Phong** trình bày **U024, U020, U022, U032 và U037**.

### Câu chuyển sau phần Nguyễn Thanh Phong

**Trên màn hình:** Khi bạn Phong kết thúc, trỏ lần lượt vào U024, U020, U022, U032, U037; sau đó chuyển con trỏ sang U034.

**Nói:**

> Cảm ơn bạn **Nguyễn Thanh Phong**. Phần vừa rồi đã chứng minh lễ tân xem được các booking `CHECKED_IN` dự kiến trả trong ngày, Housekeeper xử lý đúng chuỗi `DIRTY → CLEANING → AVAILABLE`, và khách nhận được câu trả lời chính sách dựa trên nguồn thông tin của khách sạn. Các kết quả này khép kín giai đoạn trả phòng–dọn phòng và bổ sung kênh hỗ trợ chính sách cho khách. Tiếp theo em mời bạn **Nguyễn Bá Thiện** trình bày **U034**.

### Câu chuyển sau phần Nguyễn Bá Thiện

**Trên màn hình:** Khi bạn Thiện kết thúc, trỏ vào U034 và trạng thái `Done`; chỉ vào trạng thái `MAINTENANCE` trên màn hình quản lý phòng, sau đó chuyển con trỏ sang U023, U026 và U031.

**Nói:**

> Cảm ơn bạn **Nguyễn Bá Thiện**. Phần vừa rồi đã chứng minh Manager có thể đưa phòng vào trạng thái `MAINTENANCE` và phòng đó không còn được sử dụng để gán cho khách. Kết quả này bảo vệ khả năng cung ứng thực tế và nối trực tiếp với quy trình chọn phòng an toàn khi check-in. Tiếp theo em xin trình bày phần cá nhân của mình gồm **U023, U026 và U031**.

Nếu thành viên gặp lỗi demo:

> Em xin chốt trạng thái: Acceptance Criteria chính đã được thể hiện trong **ảnh hoặc video dự phòng**; lỗi hiện tại thuộc môi trường demo và không làm thay đổi Jira status đã chốt. Để bảo đảm thời lượng, nhóm chuyển sang phần tiếp theo và sẽ quay lại nếu còn thời gian.

### Chuyển sang phần cá nhân của leader

> Cảm ơn phần trình bày của các bạn. Tiếp theo em báo cáo phần việc cá nhân. Em sẽ bắt đầu bằng U023 và U026 vì hai story này nối thành một transaction nghiệp vụ: chọn phòng vật lý phù hợp và check-in khách.

---

## 5. Demo cá nhân — U023 và U026

### 5.1 Giới thiệu Acceptance Criteria

**Trên màn hình:** Chuyển sang browser profile `RECEPTIONIST`, đang đăng nhập. Mở menu dashboard.

**Nói:**

> Với U023, Acceptance Criteria quan trọng là hệ thống chỉ liệt kê phòng vật lý đang AVAILABLE và đúng hạng phòng của booking CONFIRMED. Với U026, khi lễ tân xác nhận, việc gán phòng và check-in phải diễn ra nhất quán: booking chuyển sang CHECKED_IN, phòng chuyển sang OCCUPIED và ghi nhận thời gian check-in. Backend khóa booking rồi khóa phòng và kiểm tra lại trạng thái trước khi cập nhật để giảm race condition.

### 5.2 Happy path — tìm và gán phòng

**Thao tác:**

1. Ở menu trái, bấm **Nhận phòng**.
2. Dừng 1–2 giây ở tiêu đề **Check-in khách**.
3. Bấm vào ô **Mã booking**.
4. Nhập `BK-S2-CHECKIN-01`.
5. Bấm **Tìm phòng khả dụng**.
6. Khi danh sách hiện ra, trỏ vào dòng phụ: “Danh sách được lọc theo hạng phòng của booking và trạng thái AVAILABLE.”
7. Chọn phòng `D-S2-01`; đọc số phòng và tầng 9 đang hiển thị. Nếu fixture chính đã được dùng, chuyển sang `BK-S2-CHECKIN-02` và `D-S2-02`.

**Nói trong lúc thao tác:**

> Em vào màn hình Nhận phòng và nhập mã booking đã được xác nhận. Khi bấm Tìm phòng khả dụng, frontend gọi API lấy phòng theo chính booking này. Danh sách không phải toàn bộ phòng của khách sạn: nó đã được lọc theo đúng hạng phòng và trạng thái AVAILABLE. Ở đây em chọn phòng **[đọc số phòng]**, tầng **[đọc số tầng]**. Đây là phần đáp ứng U023.

### 5.3 Happy path — check-in thành công

**Thao tác:**

1. Bấm **Xác nhận check-in** đúng một lần.
2. Chờ toast “Check-in thành công, phòng …”.
3. Trỏ vào card **Check-in thành công** bên phải.
4. Trỏ lần lượt vào: mã booking, số phòng, tag booking **Đã nhận phòng**, tag phòng **Đang sử dụng**, thời gian.

**Nói:**

> Em xác nhận check-in. Kết quả trả về cho thấy mã booking và phòng đã gán. Booking hiện là Đã nhận phòng, tương ứng `CHECKED_IN`; phòng là Đang sử dụng, tương ứng `OCCUPIED`; và hệ thống lưu thời gian thực hiện. Hai thay đổi nằm trong cùng transaction nên không có trạng thái booking đã check-in nhưng phòng vẫn available. Như vậy happy path của U023 và U026 đã đáp ứng đầy đủ các Acceptance Criteria chính.

### 5.4 Unhappy path — booking không hợp lệ

**Lưu ý:** Thực hiện case này trước happy path nếu chỉ có một booking demo và muốn tránh reset; hoặc bấm **Làm mới** sau happy path rồi dùng mã khác.

**Thao tác:**

1. Bấm **Làm mới**.
2. Nhập `BK-S2-NOT-FOUND`.
3. Bấm **Tìm phòng khả dụng**.
4. Trỏ vào alert **Không thể tải phòng khả dụng** và nội dung **Không tìm thấy booking với mã đã nhập.**

**Nói:**

> Đây là alternative path đầu tiên. Nếu mã booking không tồn tại, hệ thống dừng ngay ở bước tải phòng và đưa ra thông báo cụ thể; lễ tân không thể chọn phòng hay gửi check-in với dữ liệu không hợp lệ.

### 5.5 Unhappy path dự phòng — sai trạng thái hoặc cạnh tranh phòng

Chỉ demo nếu còn thời gian và đã chuẩn bị dữ liệu chắc chắn.

**Phương án A — booking chưa xác nhận:**

1. Bấm **Làm mới**.
2. Nhập `BK-S2-PENDING`.
3. Bấm **Tìm phòng khả dụng**.

**Nói:**

> Booking chưa CONFIRMED bị từ chối với thông báo “Booking chưa được xác nhận nên chưa thể check-in”. Đây là guard bảo vệ quy trình thanh toán và trạng thái booking.

**Phương án B — hai lễ tân chọn cùng phòng:**

> Danh sách phòng chỉ là snapshot, không giữ phòng. Khi xác nhận, backend khóa và kiểm tra lại. Nếu phòng vừa được lễ tân khác sử dụng, request sau nhận `ROOM_NOT_AVAILABLE` thay vì ghi đè. Đây là cơ chế bảo vệ trong implementation; em không tạo race condition trực tiếp trong buổi report vì U035 vẫn đang `InProgress`.

Không cố tạo race condition trực tiếp trên UI nếu chưa rehearsal ổn định.

### 5.6 Câu chốt U023/U026

> Kết quả của hai story là lễ tân chỉ gán được phòng hợp lệ và trạng thái booking–phòng được cập nhật đồng bộ. Phần này trực tiếp đáp ứng Sprint Goal về rút ngắn quy trình nhận phòng và bảo vệ tính nhất quán vận hành.

---

## 6. Demo cá nhân — U031 AI Room Recommendation

### 6.1 Giới thiệu Acceptance Criteria

**Trên màn hình:** Chuyển sang cửa sổ `GUEST` đã đăng nhập, ở trang chủ hoặc trang tìm phòng.

**Nói:**

> U031 hỗ trợ khách tìm nhanh hạng phòng phù hợp bằng hội thoại. Acceptance Criteria chính là AI hiểu yêu cầu về ngày lưu trú và số khách, dựa trên dữ liệu phòng và khả năng cung ứng hiện tại để đề xuất; sau đó tạo hành động đưa khách sang form booking đã điền sẵn. AI không tự đặt phòng thay khách.

### 6.2 Happy path — nhận đề xuất và tiếp tục đặt phòng

**Thao tác:**

1. Bấm nút robot góc phải, tooltip **Trợ lý HANBAY AI**.
2. Nếu hội thoại cũ đang mở, bấm **Chat mới**.
3. Trong ô “Hỏi về chính sách, phòng, giá hoặc booking...”, nhập câu đã rehearsal:

   `Tìm phòng phù hợp cho 2 người lớn, từ 10/09/2026 đến 13/09/2026. Tôi ưu tiên phòng còn trống và muốn biết giá.`

4. Bấm biểu tượng gửi.
5. Khi AI stream câu trả lời, không bấm liên tục; chờ nút **Tiếp tục đặt phòng** xuất hiện.
6. Chỉ vào phần đề xuất, nhắc lại hạng phòng/giá/điều kiện mà màn hình thực tế trả về.
7. Bấm **Tiếp tục đặt phòng**.
8. Ở trang tạo booking, trỏ vào ngày nhận, ngày trả, số khách và hạng phòng đã được điền.

**Nói:**

> Em cung cấp đủ ngày và số khách để AI có thể kiểm tra đúng ngữ cảnh. Kết quả hiện tại đề xuất **[đọc đúng tên hạng phòng]** với **[đọc thông tin giá hoặc lý do phù hợp trên màn hình]**. Quan trọng là AI tạo nút Tiếp tục đặt phòng chứ chưa tạo booking. Khi em bấm nút này, hệ thống điều hướng sang bước booking và truyền sẵn hạng phòng, ngày lưu trú và số khách. Khách vẫn là người kiểm tra thông tin và xác nhận chính thức.

### 6.3 Unhappy/alternative path — yêu cầu thiếu dữ liệu

**Thao tác:**

1. Mở lại AI và bấm **Chat mới**.
2. Nhập: `Tìm cho tôi một phòng đẹp.`
3. Gửi và chờ phản hồi.

**Nói:**

> Với yêu cầu thiếu ngày và số khách, hệ thống không nên suy đoán rồi tạo booking. AI cần hỏi thêm dữ liệu hoặc chỉ tư vấn ở mức tổng quát. Đây là guard quan trọng để đề xuất không dựa trên availability sai.

Nếu AI trả lời không đúng như rehearsal, nói:

> Kết quả AI có tính xác suất. Guard quyết định cuối cùng vẫn nằm ở API availability và form booking; AI không có quyền tự tạo booking. Nhóm ghi nhận output này để bổ sung eval/prompt test.

### 6.4 Câu chốt U031

> U031 rút ngắn từ nhu cầu tự nhiên của khách đến một lựa chọn có thể đặt, nhưng vẫn giữ bước xác nhận và luật availability ở hệ thống nghiệp vụ. Đây là cách nhóm dùng AI để hỗ trợ quyết định, không thay thế business rule.

---

## 7. Tổng kết buổi báo cáo

### 14:30–15:15 — Đối chiếu Sprint Goal

**Trên màn hình:** Quay lại slide Sprint Goal và checklist AC.

**Nói:**

> Tổng kết Sprint 2, nhóm đã demo được core flow chính: hệ thống nhận booking đã xác nhận, chỉ cho chọn phòng AVAILABLE đúng hạng, check-in và cập nhật đồng bộ booking–phòng; các phần check-out và housekeeping đã được các thành viên trình bày theo ownership. AI đã đưa yêu cầu lưu trú sang đề xuất và bước booking có kiểm soát. Các nhánh lỗi quan trọng cũng chặn booking không hợp lệ và phòng không còn trống.

### 15:15–15:45 — Phần chưa hoàn thành và hành động

**Nói:**

> U034 đã Done. Nhóm còn U036 ở To-do và U035 ở InProgress. Trong buổi report hôm nay em không trình bày riêng phần test, nhưng vẫn giữ nguyên Jira status và không coi Sprint đã hoàn thành 100 phần trăm. Hành động tiếp theo là hoàn tất U035 và phân công U036 cho phần deploy/final demo preparation.

### 15:45–16:00 — Kết thúc

**Nói:**

> Phần báo cáo Sprint 2 của nhóm em đến đây là hết. Em xin cảm ơn thầy/cô và các bạn. Nhóm em sẵn sàng nhận câu hỏi.

---

## 8. Bảng đối chiếu riêng cho Trần Phước Anh

| Story | Kết quả trình bày | Acceptance Criteria được chứng minh | Bằng chứng |
|---|---|---|---|
| U023 | Lấy danh sách và chọn phòng vật lý | Chỉ phòng `AVAILABLE`, đúng hạng booking | UI **Tìm phòng khả dụng**, danh sách số phòng/tầng |
| U026 | Check-in booking xác nhận | Booking `CHECKED_IN`, room `OCCUPIED`, có timestamp; chặn dữ liệu sai | Card **Check-in thành công** và alert lỗi |
| U031 | AI đề xuất phòng | Hiểu ngày/số khách, đề xuất theo availability, chuyển sang booking, không tự book | HANBAY AI, nút **Tiếp tục đặt phòng**, form prefill |

---

## 9. Câu trả lời ngắn cho Q&A có khả năng được hỏi

**Hỏi: U023 và U026 có phải hai thao tác độc lập không?**

> Về user story có thể tách để theo dõi, nhưng implementation check-in gán phòng và đổi trạng thái trong cùng transaction. Nếu bất kỳ kiểm tra nào thất bại thì toàn bộ cập nhật rollback.

**Hỏi: Vì sao danh sách hiện AVAILABLE mà xác nhận vẫn có thể lỗi?**

> Danh sách là snapshot tại thời điểm đọc. Một lễ tân khác có thể lấy phòng trước. Vì vậy API xác nhận phải khóa room và kiểm tra lại trạng thái; request sau bị từ chối bằng `ROOM_NOT_AVAILABLE`.

**Hỏi: AI có thể gây overbooking không?**

> Không trực tiếp. AI chỉ đề xuất và điều hướng. Availability và việc tạo booking vẫn do service nghiệp vụ kiểm tra; AI không tự ghi booking.

**Hỏi: Nếu AI trả lời sai thì sao?**

> UI có cảnh báo AI có thể mắc lỗi, còn quyết định đặt phòng phải qua form và API nghiệp vụ. Nhóm tiếp tục bổ sung eval cases, grounding và theo dõi output thất bại.

**Hỏi: Leader đóng góp gì ngoài code?**

> Em điều phối task theo business flow, thống nhất contract giữa frontend–backend, theo dõi dependency giữa check-in, check-out và housekeeping, review Jira status dựa trên Acceptance Criteria, đồng thời chuẩn bị integration/demo plan. Phần trực tiếp trình bày của em là U023, U026 và U031.

---

## 10. Checklist 2 phút trước khi bắt đầu

- [ ] Đúng tài khoản receptionist và guest; không hiển thị mật khẩu.
- [ ] `BK-S2-CHECKIN-01` đang `CONFIRMED`, đúng ngày và chưa từng check-in; `BK-S2-CHECKIN-02` sẵn sàng làm bản dự phòng.
- [ ] Có ít nhất một room `AVAILABLE` đúng hạng.
- [ ] Khoảng ngày AI nằm trong tương lai và còn inventory.
- [ ] Ảnh/video dự phòng mở sẵn nhưng chưa share.
- [ ] Timer 16 phút đã bật.
- [ ] Danh sách thứ tự member và task đặt cạnh màn hình.
- [ ] U034 đã `Done`; U036 vẫn `To-do`; U035 vẫn `InProgress`, nhưng không dành một phần báo cáo test riêng.
