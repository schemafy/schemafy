-- KEYS[1] participantsKey
-- KEYS[2] expiresKey
-- ARGV[1] sessionId
-- ARGV[2] now

local score = redis.call('ZSCORE', KEYS[2], ARGV[1])

if not score or tonumber(score) > tonumber(ARGV[2]) then
  return nil
end

local payload = redis.call('HGET', KEYS[1], ARGV[1])
redis.call('HDEL', KEYS[1], ARGV[1])
redis.call('ZREM', KEYS[2], ARGV[1])
return payload
