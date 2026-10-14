local dueKey = KEYS[1]
local jobKey = KEYS[2]
local requestedJobKey = ARGV[1]
local leaseToken = ARGV[2]
local now = tonumber(ARGV[3])
local leaseTtlMillis = tonumber(ARGV[4])
local ttlMillis = tonumber(ARGV[5])

local score = redis.call('ZSCORE', dueKey, jobKey)
if not score or tonumber(score) > now or redis.call('EXISTS', jobKey) == 0 then
  return nil
end

local leaseUntil = tonumber(redis.call('HGET', jobKey, 'leaseUntil') or '0')
if leaseUntil > now then
  return nil
end

local newLeaseUntil = now + leaseTtlMillis
redis.call('HSET', jobKey, 'leaseToken', leaseToken, 'leaseUntil', newLeaseUntil)
redis.call('ZADD', dueKey, newLeaseUntil, jobKey)
redis.call('PEXPIRE', jobKey, ttlMillis)

return cjson.encode({
  jobKey = requestedJobKey,
  projectId = redis.call('HGET', jobKey, 'projectId'),
  schemaId = redis.call('HGET', jobKey, 'schemaId'),
  kind = redis.call('HGET', jobKey, 'kind'),
  targetRevision = tonumber(redis.call('HGET', jobKey, 'targetRevision')),
  generation = tonumber(redis.call('HGET', jobKey, 'generation') or '0'),
  leaseToken = leaseToken,
  failureCount = tonumber(redis.call('HGET', jobKey, 'failureCount') or '0')
})
