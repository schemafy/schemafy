import { useContext, useState } from 'react';
import { observer } from 'mobx-react-lite';
import { ThemeProviderContext, type Theme } from '@/lib/config';
import {
  Avatar,
  Button,
  InputField,
  RadioGroup,
  RadioGroupItem,
} from '@/components';
import { authStore } from '@/store/auth.store';

export const SettingsPage = observer(() => {
  const { theme, setTheme } = useContext(ThemeProviderContext);
  const user = authStore.user;

  const [name, setName] = useState(user?.name ?? '');
  const [email, setEmail] = useState(user?.email ?? '');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  return (
    <div className="flex w-full justify-center py-12 px-8">
      <div className="w-full max-w-4xl flex gap-12">
        <div className="flex flex-col items-center gap-3 shrink-0">
          <Avatar
            src="https://picsum.photos/200/300?random=1"
            className="w-40 h-40"
          />
          <button
            type="button"
            className="font-body-md text-schemafy-text"
          >
            Edit
          </button>
        </div>

        <form className="flex flex-1 flex-col gap-2">
          <InputField
            label="Name"
            name="name"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <InputField
            label="Email"
            name="email"
            type="email"
            value={email}
            placeholder="schemafy@email.com"
            onChange={(e) => setEmail(e.target.value)}
          />
          <InputField
            label="Password"
            name="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <InputField
            label="Confirm Password"
            name="confirmPassword"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
          />

          <div className="flex flex-col gap-2 py-3 px-4">
            <span className="font-overline-md text-schemafy-text">Theme</span>
            <RadioGroup
              value={theme}
              onValueChange={(value) => setTheme(value as Theme)}
              className="flex-row"
            >
              <RadioGroupItem value="light">Light</RadioGroupItem>
              <RadioGroupItem value="dark">Dark</RadioGroupItem>
            </RadioGroup>
          </div>

          <div className="flex flex-col gap-3 px-4 pt-2">
            <Button type="submit" fullWidth round>
              Save
            </Button>
            <Button type="button" variant="outline" fullWidth round>
              Delete Account
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
});
