import {
  createRootRouteWithContext,
  createRoute,
  createRouter,
  Outlet,
  redirect,
  useRouterState,
} from '@tanstack/react-router';
import type { QueryClient } from '@tanstack/react-query';
import { cn } from '@/lib';
import { Header } from '@/components/Header';
import { Footer } from '@/components/Footer';
import { Toaster } from '@/components/Toaster';
import {
  CanvasPage,
  LandingPage,
  OAuthCallbackPage,
  SignInPage,
  SignUpPage,
  WorkspacePage,
} from '@/pages';
import type { authStore } from '@/store/auth.store';
import { ensureAuthInitialized } from '@/features/auth/lib/auth-bootstrap';

export interface RouterContext {
  queryClient: QueryClient;
  auth: typeof authStore;
}

// 루트 레이아웃
const RootLayout = () => {
  const pathname = useRouterState({
    select: (s) => s.location.pathname,
  });
  const isCanvasPage = pathname.startsWith('/project/');
  const isWorkspacePage = pathname.startsWith('/workspace');

  return (
    <div className="layout flex flex-col min-h-screen bg-schemafy-bg w-full items-center">
      <Header isCanvasPage={isCanvasPage} />
      <main
        className={cn(
          'flex-grow w-full flex',
          !isCanvasPage && !isWorkspacePage && 'max-w-[960px]',
        )}
      >
        <Outlet />
      </main>
      {!isCanvasPage && !isWorkspacePage && <Footer />}
      <Toaster />
    </div>
  );
};

const rootRoute = createRootRouteWithContext<RouterContext>()({
  component: RootLayout,
  notFoundComponent: () => (
    <div className="flex flex-col items-center justify-center py-16 gap-4">
      <h1 className="font-heading-xl">404</h1>
      <p className="text-schemafy-dark-gray">페이지를 찾을 수 없습니다.</p>
    </div>
  ),
});

const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: LandingPage,
});

const signinRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/signin',
  component: SignInPage,
});

const signupRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/signup',
  component: SignUpPage,
});

const oauthCallbackRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/oauth/callback',
  component: OAuthCallbackPage,
});

const requireAuth = async () => {
  const authenticated = await ensureAuthInitialized();

  if (!authenticated) {
    throw redirect({
      to: '/signin',
      replace: true,
    });
  }
};

const workspaceRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/workspace',
  beforeLoad: requireAuth,
  component: WorkspacePage,
});

const projectRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/project/$projectId',
  beforeLoad: requireAuth,
  component: CanvasPage,
});

const routeTree = rootRoute.addChildren([
  indexRoute,
  signinRoute,
  signupRoute,
  oauthCallbackRoute,
  workspaceRoute,
  projectRoute,
]);

export const router = createRouter({
  routeTree,
  defaultPreload: 'intent',
  context: undefined!,
});

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router;
  }
}
