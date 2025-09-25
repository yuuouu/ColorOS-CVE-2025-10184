> [信息来源](https://www.rapid7.com/blog/post/cve-2025-10184-oneplus-oxygenos-telephony-provider-permission-bypass-not-fixed/)  

### 漏洞说明

- 类型：数据库注入攻击"1=1 AND"
- 效果：任意APP读取短信数据。无需权限、无需用户交互、读取无感知
- 影响范围：21年OxygenOS 12至今最新的ColorOs 15系统
- 修复情况：厂商已回应rapid7（漏洞发现者）说会修复，9-25暂未修复
- 解决方案：普通用户只能看完短信之后及时删除，要彻底删除
- 漏洞影响：本机的所有短信信息、短信验证码泄露
- 本机测试：安装apk执行即可看到数据，如果看不到数据说明你的手机没有被影响

### 实际效果

<img src="http://upforme.ru/uploads/001c/43/d3/2/601056.jpg" width="260"><img src="http://upforme.ru/uploads/001c/43/d3/2/42952.jpg" width="260"><img src="http://upforme.ru/uploads/001c/43/d3/2/500951.jpg" width="260">

### 字段含义

`SELECT address FROM sms ORDER BY rowid DESC LIMIT 3`
- adddress：短信发送方或接收方号码，可修改为下面的任意字段
- LIMIT 3：最近的 3 个

| 字段名                            | 含义说明                       |
| ------------------------------ | -------------------------- |
| _id                            | 主键，自增 ID                   |
| thread_id                      | 会话 ID，归属到哪个对话线程            |
| address                        | 短信发送方或接收方号码                |
| person                         | 联系人 ID（可能对应 Contacts 表）    |
| date                           | 收到/发送时间（毫秒时间戳）             |
| date_sent                      | 实际发送时间（对发出短信有效）            |
| protocol                       | 协议：0=SMS, 1=MMS            |
| read                           | 是否已读（0=未读, 1=已读）           |
| status                         | 发送状态（-1=接收, 0=成功, 64=待发送等） |
| type                           | 短信类型（1=收件箱, 2=已发件, 3=草稿等）  |
| reply_path_present             | 是否设置了回复路径                  |
| subject                        | 短信主题（MMS 可能有）              |
| body                           | 短信正文内容                     |
| service_center                 | 服务中心号码 (SMSC)              |
| locked                         | 是否锁定（防止被系统清理）              |
| sub_id                         | 使用的 SIM 卡 ID               |
| phone_id                       | 手机卡槽 ID                    |
| error_code                     | 发送错误码                      |
| creator                        | 创建该短信的应用包名                 |
| seen                           | 是否已在界面展示（0/1）              |
| priority                       | 短信优先级（厂商/IMS扩展）            |
| m_size                         | 消息大小（多见于 MMS）              |
| oplus_drafts                   | Oplus 定制：草稿标记              |
| oplus_mass                     | Oplus 定制：群发标记              |
| oplus_timer                    | Oplus 定制：定时短信标记            |
| oplus_groupaddress             | Oplus 定制：群组地址              |
| oplus_collected                | Oplus 定制：收藏状态              |
| oplus_sub_date                 | Oplus 定制：订阅/时间戳            |
| oplus_service_message_sms_type | Oplus 定制：服务短信类型            |
| bubble                         | 是否气泡显示（某些 UI 特性）           |
| deleted                        | 删除标记（0/1）                  |
| sync_state                     | 同步状态（云端/备份相关）              |
| sync_id                        | 同步用的唯一 ID                  |
| oplus_message_url              | Oplus 定制：消息中的 URL          |
| oplus_sms_type                 | Oplus 定制：短信类型扩展            |
| block_type                     | 拦截类型（垃圾短信/骚扰拦截）            |
| favourite                      | 收藏标记                       |
| rcs_message_id                 | RCS 消息 ID                  |
| rcs_file_name                  | RCS 附件文件名                  |
| rcs_mime_type                  | RCS 附件 MIME 类型             |
| rcs_msg_type                   | RCS 消息类型（文本/文件/地理位置等）      |
| rcs_msg_state                  | RCS 消息状态（已读/已送达/发送中等）      |
| rcs_chat_type                  | RCS 会话类型（1对1/群聊）           |
| rcs_conversation_id            | RCS 会话 ID                  |
| rcs_contribution_id            | RCS 消息贡献 ID                |
| rcs_file_selector              | RCS 文件选择器标记                |
| rcs_file_transfered            | RCS 文件是否已传输完成              |
| rcs_file_transfer_id           | RCS 文件传输 ID                |
| rcs_file_icon                  | RCS 文件图标路径                 |
| rcs_burn                       | RCS 阅后即焚标记                 |
| rcs_header                     | RCS 消息头信息                  |
| rcs_file_path                  | RCS 文件路径                   |
| rcs_is_download                | RCS 文件是否已下载                |
| rcs_file_size                  | RCS 文件大小                   |
| rcs_thumb_path                 | RCS 缩略图路径                  |
| rcs_extend_body                | RCS 扩展消息体（JSON/XML等）       |
| rcs_media_played               | RCS 媒体是否播放过                |
| rcs_ext_contact                | RCS 扩展联系人信息                |
| rcs_file_record                | RCS 文件传输记录                 |
| rcs_transfer_date              | RCS 文件传输时间                 |
| rcs_group_at_reminds           | RCS 群聊 @提醒                 |
| rcs_audio_read                 | RCS 音频已读状态                 |
