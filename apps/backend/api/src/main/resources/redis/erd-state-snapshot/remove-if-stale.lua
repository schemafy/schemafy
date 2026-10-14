local dueKey = KEYS[1]
local jobKey = KEYS[2]

if redis.call('EXISTS', jobKey) == 1 then
  return 1
end

redis.call('ZREM', dueKey, jobKey)
return 0
