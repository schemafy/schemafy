local jobKey = KEYS[1]
local leaseToken = ARGV[1]
local generation = tonumber(ARGV[2])
local kind = ARGV[3]
local candidateRevision = tonumber(ARGV[4])

if redis.call('EXISTS', jobKey) == 0 then
  return 0
end
if redis.call('HGET', jobKey, 'leaseToken') ~= leaseToken then
  return 0
end
if tonumber(redis.call('HGET', jobKey, 'generation') or '-1') ~= generation then
  return 0
end
if redis.call('HGET', jobKey, 'kind') ~= kind then
  return 0
end

local targetRevision = tonumber(redis.call('HGET', jobKey, 'targetRevision') or '-1')
local publishedRevision = tonumber(redis.call('HGET', jobKey, 'publishedRevision') or '-1')
local deletedRevision = tonumber(redis.call('HGET', jobKey, 'deletedRevision') or '-1')
if candidateRevision < targetRevision or candidateRevision <= publishedRevision then
  return 0
end
if kind == 'ACTIVE' and candidateRevision <= deletedRevision then
  return 0
end
return 1
