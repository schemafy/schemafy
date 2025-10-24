import { Button } from '@/components';
import { Avatar } from '@radix-ui/react-avatar';

export const Memo = () => {
  return (
    <div>
      <div>
        <div className="memo-icon">
          <Avatar />
        </div>
        <Input />
      </div>
      <Contents />
    </div>
  );
};

const Contents = () => {
  return (
    <div>
      <ul>
        <li></li>
      </ul>
      <div>
        <Avatar />
        <Input />
      </div>
    </div>
  );
};

const Input = () => {
  return (
    <div>
      <input />
      <Button round size={'dropdown'}></Button>
    </div>
  );
};
