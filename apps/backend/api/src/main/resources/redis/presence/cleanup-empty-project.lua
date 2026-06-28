-- KEYS[1] activeProjectsKey
-- KEYS[2] participantsKey
-- KEYS[3] expiresKey
-- ARGV[1] projectId

if redis.call('HLEN', KEYS[2]) > 0 then
  return 0
end

redis.call('SREM', KEYS[1], ARGV[1])
redis.call('DEL', KEYS[2], KEYS[3])
return 1
