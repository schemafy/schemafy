import {
  createRootRouteWithContext,
  createRoute,
  createRouter,
  redirect,
} from '@tanstack/react-router';
import type { QueryClient } from '@tanstack/react-query';
import { Layout } from '@/components';
import {
  CanvasPage,
  LandingPage,
  NotFoundPage,
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

const rootRoute = createRootRouteWithContext<RouterContext>()({
  component: Layout,
  notFoundComponent: NotFoundPage,
});

const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: LandingPage,
});

const signinRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/signin',
  validateSearch: (search: Record<string, unknown>) => ({
    oauthError: typeof search.oauthError === 'string' ? search.oauthError : null,
  }),
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
  validateSearch: (search: Record<string, unknown>) => ({
    error: typeof search.error === 'string' ? search.error : null,
  }),
  component: OAuthCallbackPage,
});

const requireAuth = async () => {
  const authenticated = await ensureAuthInitialized();

  if (!authenticated) {
    throw redirect({
      to: '/signin',
      replace: true,
      search: { oauthError: null },
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
