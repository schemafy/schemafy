local dueKey = KEYS[1]
local jobKey = KEYS[2]
local leaseToken = ARGV[1]
local generation = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local leaseTtlMillis = tonumber(ARGV[4])
local ttlMillis = tonumber(ARGV[5])

if redis.call('HGET', jobKey, 'leaseToken') ~= leaseToken then
  return 0
end
if tonumber(redis.call('HGET', jobKey, 'generation') or '-1') ~= generation then
  return 0
end

local leaseUntil = now + leaseTtlMillis
redis.call('HSET', jobKey, 'leaseUntil', leaseUntil)
redis.call('ZADD', dueKey, leaseUntil, jobKey)
redis.call('PEXPIRE', jobKey, ttlMillis)
return 1
