import { Avatar } from '@/components';
import { useAuthStore } from '@/store';

export const UserMenu = () => {
  const user = useAuthStore((s) => s.user);

  if (!user) {
    return null;
  }

  return (
    <div className="flex items-center gap-3">
      <span className="font-body-sm text-schemafy-dark-gray">{user.name}</span>
      <Avatar src="https://picsum.photos/200/300?random=2" />
    </div>
  );
};
