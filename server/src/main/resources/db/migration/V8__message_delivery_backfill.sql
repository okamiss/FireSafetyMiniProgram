UPDATE station_message
SET external_status = 'SKIPPED',
    external_error_code = 'LEGACY_MESSAGE',
    external_error_message = '该消息生成于微信订阅消息接入前，未执行外部发送'
WHERE external_status = 'PENDING';
