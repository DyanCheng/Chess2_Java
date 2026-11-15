---
target: trải nghiệm toàn bộ Chess² như một người chơi
total_score: 26
max_score: 40
na_heuristics:
p0_count: 0
p1_count: 2
target_identity: "file:F:\\code\\Chess2_Java\\apps\\web\\index.html"
target_fingerprint: "sha256:b98fe541377d23e196553e9afec332f4e266494443f4a66b1d4767bbf9092d70"
target_path: "F:\\code\\Chess2_Java\\apps\\web\\index.html"
timestamp: 2026-11-14T09-25-00Z
slug: apps-web-index-html
---
# Đánh giá trải nghiệm người chơi Chess²

## Design Health Score

| # | Heuristic | Điểm | Vấn đề chính |
|---|---|---:|---|
| 1 | Visibility of System Status | 3 | Trạng thái lượt, bot, HP và nhắm mục tiêu rõ; kết quả trận lại mơ hồ. |
| 2 | Match System / Real World | 3 | Ngôn ngữ cờ/thẻ tự nhiên; tên nội bộ tiếng Anh vẫn lọt vào accessibility. |
| 3 | User Control and Freedom | 3 | Có Esc, hủy chọn, về sảnh và tiếp tục; refresh không tự dẫn về ván đang chơi. |
| 4 | Consistency and Standards | 3 | Hệ thị giác nhất quán; tên thẻ hiển thị và tên được screen reader đọc chưa đồng nhất. |
| 5 | Error Prevention | 2 | Có thể tiêu thẻ Hồi phục tại 100/100 HP mà không nhận lợi ích. |
| 6 | Recognition Rather Than Recall | 3 | Luật, effect, journal hiện tại chỗ; recovery lại buộc người dùng nhớ đường qua Hồ sơ. |
| 7 | Flexibility and Efficiency | 2 | Click, drag, Esc và lật bàn hoạt động; thiếu accelerator/filter cho người chơi thường xuyên. |
| 8 | Aesthetic and Minimalist Design | 3 | Màn chơi tập trung tốt; lobby và shop vẫn nhiều lựa chọn ngang cấp. |
| 9 | Error Recovery | 2 | Ván còn trên server nhưng route Chơi cờ sau refresh trông như đã mất. |
| 10 | Help and Documentation | 2 | Luật trong trận hữu ích; thiếu onboarding ngắn cho HP, consumable và economy. |
| **Tổng** |  | **26/40** | **Acceptable — nền tảng tốt nhưng còn điểm gãy niềm tin trước khi phát hành rộng.** |

## Design Specificity Verdict

### LLM assessment

Giao diện có bản sắc riêng và không thể đổi logo để trở thành một sản phẩm bất kỳ. Nền forest/midnight, parchment, gold, pixel wordmark, quân cờ minh họa, HP và hand thẻ tạo đúng cảm giác “arcade chess/card-battler”. Màn PvE desktop là phần mạnh nhất: bàn cờ là tiêu điểm, HP và lượt luôn thấy, thẻ nằm ở rail phụ.

### Deterministic scan

Detector trả 22 findings thuộc 10 rule. Các finding đáng xử lý nhất là 7 trường hợp `undersized-ui-text` ở nhãn nav 8px/HP 9px tại breakpoint hẹp, 1 `tiny-text` và 1 `layout-transition` thực sự ảnh hưởng thanh HP. Nhiều finding còn lại là false positive vì selector bị cascade ẩn hoặc ghi đè: `kicker-above-heading`, `hero-eyebrow-chip`, `wide-tracking`, `radial-halo`; `pulsing-dot` hợp ngữ cảnh vì chỉ báo bot/phòng đang chờ. Detector chỉ trả line 0 cho `index.html`, nên vị trí được đối chiếu thủ công sang CSS.

### Visual overlays

Preflight có thể đổi title và append script, nhưng CSP `script-src 'self'` chặn cả inline marker lẫn `detect.js` từ live server. Không có overlay đáng tin cậy hiển thị cho người dùng. Fallback là detector JSON, Playwright headless và ảnh desktop/mobile.

## Overall Impression

Chess² đã vượt mức prototype “có màn hình”: đăng nhập khách, shop/deck, PvE chọn phe, click/drag, HP/thẻ, local, puzzle, PvP hai phiên và mobile đều chạy được trong demo local. Điểm mạnh là game có bản sắc và thao tác quân không còn thô. Điểm yếu lớn nhất không phải thiếu hiệu ứng, mà là các state quan trọng làm người chơi mất niềm tin: vật phẩm có thể bị tiêu vô ích, refresh khiến họ tưởng mất ván, và màn kết thúc né nói rõ thắng/thua.

## What's Working

1. **Core chess interaction đủ tự nhiên để chơi:** click `e7→e5` và kéo `g8→f6` đều cập nhật đúng, journal ghi nhận, board trả về trạng thái rảnh. PvE phe Đen lật bàn đúng và bot đi trước.
2. **Card targeting đã có game feel:** khi chọn Tốt xung phong, card được nhấn mạnh, UI hướng dẫn chọn Tốt, có hủy bằng Esc và hiệu ứng được phản ánh trên bàn/journal. Cường kích giảm HP từ 100 xuống 75 như kỳ vọng.
3. **PvP đã dễ hiểu hơn:** mã phòng sáu số, chủ phòng và ELO, countdown 05:00, lời mời và trạng thái chờ riêng biệt đều hiện rõ. Hai browser context nhìn thấy đúng tên/ELO và nước `e2→e4` đồng bộ sang đối thủ.

## Priority Issues

### [P1] Thẻ Hồi phục bị tiêu hao khi HP đã đầy

**Why it matters:** Người chơi bỏ Gold cho consumable hữu hạn nhưng có thể mất thẻ ở 100/100 HP và nhận zero value. Đây là lỗi niềm tin, không chỉ là thiếu polish.

**Fix:** Disable card khi không tạo lợi ích; ghi rõ “HP đã đầy”. Nếu thiết kế cố ý cho phép lãng phí, phải xác nhận trước khi tiêu.

**Suggested command:** `$impeccable harden`

### [P1] Refresh làm ván đang chơi trông như đã mất

**Why it matters:** Sau reload tại `#/play`, người chơi thấy bàn trống và “Chọn một chế độ để bắt đầu”, trong khi ván vẫn nằm ở Hồ sơ → Tiếp tục. Mobile user rất dễ gặp trạng thái này khi chuyển app/tab.

**Fix:** Tự hydrate active match trên route Chơi cờ hoặc đặt banner ngay trên bàn trống: “Bạn có một ván đang chơi — Tiếp tục”.

**Suggested command:** `$impeccable harden`

### [P2] Kết thúc trận không nói thẳng thắng/thua

**Why it matters:** Sau Đình chiến, title “Một ván đấu đáng nhớ” và ELO giảm 16 buộc người chơi tự suy luận rằng mình thua. Đoạn kết thiếu closure và làm peak–end hụt.

**Fix:** Hiển thị “Bạn đã thua/Bot chiến thắng”, nguyên nhân, rating delta, phần thưởng và một recap ngắn về HP/thẻ.

**Suggested command:** `$impeccable clarify`

### [P2] Shop và lobby gây quá tải lựa chọn cho người mới

**Why it matters:** Shop đưa 15 CTA mua ngang cấp; lobby đồng thời có hero CTA, ba mode, khu phòng chờ và join. Người mới chưa hiểu economy nhưng đã phải quyết định tiêu 250 Gold.

**Fix:** Nhóm thẻ theo Hồi phục/Tấn công/Kiểm soát/Biến đổi, gợi ý starter deck, đưa catalog nâng cao sau progressive disclosure. Giữ một CTA chính trên lobby.

**Suggested command:** `$impeccable distill`

### [P2] Accessibility copy và cỡ chữ mobile chưa đạt độ tin cậy

**Why it matters:** Screen reader có thể đọc “Healing Spring”, “Đi Tốt”, “VCKBLA” thay vì Hồi phục/Tốt xung phong/Đình chiến. Detector cũng bắt nhãn nav 8px và HP 9px ở breakpoint hẹp.

**Fix:** Dùng chung một vocabulary tiếng Việt cho visual, ARIA, journal và inventory; tăng supporting text quan trọng lên tối thiểu 12–14px và giữ touch target hiện tại.

**Suggested command:** `$impeccable adapt`

## Cognitive Load

Mức vừa: thất bại 3/8 mục — chunking, minimal choices và progressive disclosure. Match có single focus, grouping và hierarchy tốt; shop có 15 lựa chọn mua cùng lúc, global nav có 6 mục, lobby có hơn bốn đường hành động trong cùng viewport.

## Emotional Journey

- **Entry:** auth và guest CTA tạo cảm giác premium, vào nhanh.
- **Anticipation:** lobby hero truyền đạt tốt fantasy “đọc vị và lật thế”.
- **Peak:** chọn phe Đen, bàn tự lật, kéo-thả và dùng Tốt xung phong.
- **Valley:** mất thẻ Hồi phục ở 100 HP; refresh trông như mất ván.
- **End:** ngôn ngữ kết trận mơ hồ, không tạo cảm giác thắng/thua rõ ràng.

## Persona Red Flags

**Jordan — người chơi mới:** lobby cho quá nhiều đường đi ngay lập tức; không có walkthrough ngắn về HP, consumable và Gold; end-state không giải thích rõ kết quả.

**Alex — power player:** click/drag/Esc tốt, nhưng shop thiếu filter/sort; refresh không tự trở lại ván; journal nhỏ và phải cuộn sớm.

**Casey — mobile:** board 374×374 vừa màn hình và bottom nav được ẩn trong play là đúng; tuy nhiên supporting labels 8–9px quá nhỏ và khu thẻ/log kéo trang rất dài.

**Người chơi cờ arcade/card-battler:** board + HP + hand tạo đúng fantasy; nhưng consumable zero-value và thiếu recap damage/card ở cuối trận làm vòng lặp deckbuilding kém thỏa mãn.

## Minor Observations

- Mua thẻ tự động lấp slot Deck là tiện, nhưng toast chỉ nói thêm vào kho, không nói đã trang bị.
- Chất lượng illustration chưa đều; vài thẻ có art riêng mạnh, vài thẻ giống placeholder.
- Waiting room desktop rõ nhưng quá nhiều khoảng trống sau card chính; có thể dùng không gian cho quy tắc trận/invite QR nhưng không nên thêm nếu không cần.
- Mobile play cho bàn cờ lớn và dễ chạm; khi Deck trống, panel thẻ chiếm nhiều chiều cao chỉ để báo trạng thái rỗng.
- Puzzle đúng/sai đều có phản hồi; chưa kiểm tra mạng chậm, expiration 5 phút thực tế và thiết bị vật lý.

## Questions to Consider

- Một consumable có bao giờ nên được phép tạo ra zero value không?
- Recovery của ván đang chơi nên là hành vi mặc định ở route Chơi cờ hay một lựa chọn trong Hồ sơ?
- Shop muốn là catalog đầy đủ hay một quầy tactical offers có chủ đích cho từng giai đoạn?
- Kết trận cần nhấn mạnh rating, phần thưởng hay câu chuyện chiến thuật của ván vừa chơi?
