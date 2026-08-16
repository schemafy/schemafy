local dueKey = KEYS[1]
local jobKey = KEYS[2]
local leaseToken = ARGV[1]
local generation = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local delayMillis = tonumber(ARGV[4])
local ttlMillis = tonumber(ARGV[5])
local incrementFailure = ARGV[6]

if redis.call('HGET', jobKey, 'leaseToken') ~= leaseToken then
  return 0
end
if tonumber(redis.call('HGET', jobKey, 'generation') or '-1') ~= generation then
  return 0
end

local failureCount = tonumber(redis.call('HGET', jobKey, 'failureCount') or '0')
if incrementFailure ~= 'false' and incrementFailure ~= '0' then
  failureCount = failureCount + 1
end
local dueAt = now + delayMillis
redis.call('HSET', jobKey,
  'leaseToken', '',
  'leaseUntil', 0,
  'failureCount', failureCount,
  'dueAt', dueAt)
redis.call('ZADD', dueKey, dueAt, jobKey)
redis.call('PEXPIRE', jobKey, ttlMillis)
return 1
