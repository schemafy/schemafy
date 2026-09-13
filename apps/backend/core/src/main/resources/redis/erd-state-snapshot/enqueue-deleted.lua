local dueKey = KEYS[1]
local jobKey = KEYS[2]
local projectId = ARGV[1]
local schemaId = ARGV[2]
local targetRevision = tonumber(ARGV[3])
local now = tonumber(ARGV[4])
local ttlMillis = tonumber(ARGV[5])

local publishedRevision = tonumber(redis.call('HGET', jobKey, 'publishedRevision') or '-1')
local deletedRevision = tonumber(redis.call('HGET', jobKey, 'deletedRevision') or '-1')
local currentTargetRevision = tonumber(redis.call('HGET', jobKey, 'targetRevision') or '-1')

if targetRevision <= math.max(publishedRevision, deletedRevision, currentTargetRevision) then
  return 0
end

local generation = tonumber(redis.call('HGET', jobKey, 'generation') or '0') + 1
redis.call('HSET', jobKey,
  'projectId', projectId,
  'schemaId', schemaId,
  'kind', 'DELETED',
  'targetRevision', targetRevision,
  'deletedRevision', targetRevision,
  'generation', generation,
  'firstPendingAt', now,
  'dueAt', now,
  'leaseToken', '',
  'leaseUntil', 0,
  'failureCount', 0)
redis.call('ZADD', dueKey, now, jobKey)
redis.call('PEXPIRE', jobKey, ttlMillis)
return now
