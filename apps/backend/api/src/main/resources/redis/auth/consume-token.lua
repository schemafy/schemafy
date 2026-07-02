-- KEYS[1] auth token key
-- ARGV[1] tokenHash

local payload = redis.call('GET', KEYS[1])

if not payload then
  return 'MISSING'
end

local parsed, token = pcall(cjson.decode, payload)

if not parsed or type(token) ~= 'table' then
  redis.call('DEL', KEYS[1])
  return 'MISSING'
end

local attemptCount = tonumber(token['attemptCount'])
local maxAttemptCount = tonumber(token['maxAttemptCount'])

if not attemptCount or not maxAttemptCount then
  redis.call('DEL', KEYS[1])
  return 'MISSING'
end

if attemptCount >= maxAttemptCount then
  redis.call('DEL', KEYS[1])
  return 'ATTEMPTS_EXCEEDED'
end

if token['tokenHash'] == ARGV[1] then
  redis.call('DEL', KEYS[1])
  return 'CONSUMED'
end

attemptCount = attemptCount + 1
token['attemptCount'] = attemptCount

if attemptCount >= maxAttemptCount then
  redis.call('DEL', KEYS[1])
  return 'ATTEMPTS_EXCEEDED'
end

local ttl = redis.call('PTTL', KEYS[1])
if ttl <= 0 then
  redis.call('DEL', KEYS[1])
  return 'MISSING'
end

redis.call('PSETEX', KEYS[1], ttl, cjson.encode(token))

return 'MISMATCH'
