local dueKey = KEYS[1]
local jobKey = KEYS[2]
local projectId = ARGV[1]
local schemaId = ARGV[2]
local targetRevision = tonumber(ARGV[3])
local now = tonumber(ARGV[4])
local debounceMillis = tonumber(ARGV[5])
local maxWaitMillis = tonumber(ARGV[6])
local ttlMillis = tonumber(ARGV[7])

local publishedRevision = tonumber(redis.call('HGET', jobKey, 'publishedRevision') or '-1')
local deletedRevision = tonumber(redis.call('HGET', jobKey, 'deletedRevision') or '-1')
local currentTargetRevision = tonumber(redis.call('HGET', jobKey, 'targetRevision') or '-1')

if targetRevision <= math.max(publishedRevision, deletedRevision, currentTargetRevision) then
  return 0
end

local firstPendingAt = tonumber(redis.call('HGET', jobKey, 'firstPendingAt') or now)
local dueAt = math.min(now + debounceMillis, firstPendingAt + maxWaitMillis)
local generation = tonumber(redis.call('HGET', jobKey, 'generation') or '0')
local leaseUntil = tonumber(redis.call('HGET', jobKey, 'leaseUntil') or '0')

redis.call('HSET', jobKey,
  'projectId', projectId,
  'schemaId', schemaId,
  'kind', 'ACTIVE',
  'targetRevision', targetRevision,
  'generation', generation,
  'firstPendingAt', firstPendingAt,
  'dueAt', dueAt,
  'failureCount', 0)
redis.call('ZADD', dueKey, math.max(dueAt, leaseUntil), jobKey)
redis.call('PEXPIRE', jobKey, ttlMillis)
return dueAt
