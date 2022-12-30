-- 1. 参数列表
-- 1.1 秒杀券id voucherId
local voucherId = ARGV[1]
-- 1.2 用户id userId
local userId = ARGV[2]
-- 1.3 订单id
local orderId = ARGV[3]

-- 2. 数据key
-- 2.1 秒杀券库存key
local stockKey = 'seckill:stock:' .. voucherId
-- 2.2 秒杀券订单key
local orderKey = 'seckill:order:' .. voucherId

-- 3. 脚本业务
-- 3.1. 判断秒杀券库存
if (tonumber(redis.call('get', stockKey)) <= 0) then
    -- 3.2 库存不足，返回1
    return 1;
end

-- 3.2. 判断用户是否已经下过单了
if(redis.call('SISMEMBER', orderKey, userId) == 1)then
    -- 3.3 已经下过单了，返回2
    return 2
end

-- 3.4. 扣库存
redis.call('incrby', stockKey, -1)
-- 3.5. 下单：将当前userId存入当前秒杀券的set集合
redis.call('sadd', orderKey, userId)
-- 3.6. 发送消息到队列中，XADD stream.orders * k1 v1 k2 v2 ...
redis.call('xadd', 'stream.orders', '*', 'userId', userId, 'voucherId', voucherId, 'id', orderId)

return 0
