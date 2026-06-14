-- KEYS[1] participantsKey
-- KEYS[2] expiresKey
-- KEYS[3] activeProjectsKey
-- ARGV[1] sessionId
-- ARGV[2] lastSeenAt
-- ARGV[3] expiresAt
-- ARGV[4] projectId

local payload = redis.call('HGET', KEYS[1], ARGV[1])

if not payload then
  return nil
end

local parsed, session = pcall(cjson.decode, payload)

if not parsed or type(session) ~= 'table' then
  return nil
end

session['lastSeenAt'] = tonumber(ARGV[2])
local refreshedPayload = cjson.encode(session)

redis.call('HSET', KEYS[1], ARGV[1], refreshedPayload)
redis.call('ZADD', KEYS[2], tonumber(ARGV[3]), ARGV[1])
redis.call('SADD', KEYS[3], ARGV[4])
return refreshedPayload
