local dueKey = KEYS[1]
local jobKey = KEYS[2]
local leaseToken = ARGV[1]
local generation = tonumber(ARGV[2])
local kind = ARGV[3]
local publishedRevision = tonumber(ARGV[4])
local now = tonumber(ARGV[5])
local ttlMillis = tonumber(ARGV[6])

if redis.call('HGET', jobKey, 'leaseToken') ~= leaseToken then
  return 0
end
if tonumber(redis.call('HGET', jobKey, 'generation') or '-1') ~= generation then
  return 0
end
if redis.call('HGET', jobKey, 'kind') ~= kind then
  return 0
end

local currentPublishedRevision = tonumber(redis.call('HGET', jobKey, 'publishedRevision') or '-1')
local targetRevision = tonumber(redis.call('HGET', jobKey, 'targetRevision') or '-1')
redis.call('HSET', jobKey,
  'publishedRevision', math.max(currentPublishedRevision, publishedRevision),
  'leaseToken', '',
  'leaseUntil', 0,
  'failureCount', 0)

if targetRevision > publishedRevision then
  local dueAt = tonumber(redis.call('HGET', jobKey, 'dueAt') or now)
  redis.call('ZADD', dueKey, math.max(dueAt, now), jobKey)
else
  redis.call('ZREM', dueKey, jobKey)
  redis.call('HDEL', jobKey, 'firstPendingAt', 'dueAt')
end
redis.call('PEXPIRE', jobKey, ttlMillis)
return 1
