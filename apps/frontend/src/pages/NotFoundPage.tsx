import { useNavigate } from 'react-router-dom';
import { Button } from '@/components';

export const NotFoundPage = () => {
  const navigate = useNavigate();

  return (
    <div className="flex flex-col items-center justify-center h-full gap-4">
      <h1 className="font-heading-lg text-schemafy-text">404</h1>
      <p className="font-body-sm text-schemafy-dark-gray">페이지를 찾을 수 없습니다.</p>
      <Button size="sm" onClick={() => navigate('/')}>
        홈으로 돌아가기
      </Button>
    </div>
  );
};