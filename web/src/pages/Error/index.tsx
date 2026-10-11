import { useNavigate } from 'react-router-dom';
import { Button } from '~/components';

type ErrorScreenProps = {
  title: string;
  description: string;
};

function ErrorScreen({ title, description }: ErrorScreenProps) {
  const navigate = useNavigate();

  return (
    <main className="flex grow flex-col items-center justify-center gap-6 p-4 text-center">
      <div className="flex flex-col items-center gap-2">
        <h1 className="font-phase text-6xl">{title}</h1>
        <p className="text-gray-600">{description}</p>
      </div>
      <Button
        text="처음으로"
        onClick={() => navigate('/', { replace: true })}
      />
    </main>
  );
}

export function NotFoundPage() {
  return <ErrorScreen title="404" description="없는 페이지입니다" />;
}

// 화면을 그리다 예외가 나면 라우터의 개발자용 기본 화면 대신 이걸 보여줌
export function ErrorPage() {
  return (
    <ErrorScreen
      title="Oops"
      description="화면을 그리는 중 문제가 생겼습니다. 다시 시도해 주세요"
    />
  );
}
