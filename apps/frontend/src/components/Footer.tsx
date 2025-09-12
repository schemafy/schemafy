import { Button } from './Button';

export const Footer = () => {
  return (
    <footer className="w-full flex justify-center">
      <div className="flex max-w-[960px] flex-col items-start grow py-10 px-5 gap-6 self-stretch">
        <div className="self-stretch flex justify-between">
          <Button
            variant={'none'}
            className="text-schemafy-dark-gray min-w-[10rem] text-[1rem]"
          >
            Product
          </Button>
          <Button
            variant={'none'}
            className="text-schemafy-dark-gray min-w-[10rem] text-[1rem]"
          >
            Pricing
          </Button>
          <Button
            variant={'none'}
            className="text-schemafy-dark-gray min-w-[10rem] text-[1rem]"
          >
            Resources
          </Button>
          <Button
            variant={'none'}
            className="text-schemafy-dark-gray min-w-[10rem] text-[1rem]"
          >
            Contact
          </Button>
        </div>
        <p className="self-stretch text-center text-schemafy-dark-gray">
          © 2024 Schemafy. All rights reserved.
        </p>
      </div>
    </footer>
  );
};
