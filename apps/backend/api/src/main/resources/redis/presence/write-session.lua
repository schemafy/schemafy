-- KEYS[1] participantsKey
-- KEYS[2] expiresKey
-- KEYS[3] activeProjectsKey
-- ARGV[1] sessionId
-- ARGV[2] payload
-- ARGV[3] expiresAt
-- ARGV[4] projectId

redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])
redis.call('ZADD', KEYS[2], tonumber(ARGV[3]), ARGV[1])
redis.call('SADD', KEYS[3], ARGV[4])
return 1
