
describe('Login Flow', () => {
  beforeEach(() => {
    cy.clearCookies();
    cy.clearLocalStorage();
    
    // 백엔드 실행이 없어도 로그인을 할 수 있도록 mock 데이터 구성
    cy.intercept('POST', '**/public/api/v1.0/users/login', {
      statusCode: 200,
      headers: {
        authorization: 'Bearer mock-access-token',
        'set-cookie': 'refreshToken=mock-refresh-token',
      },
      body: {
        success: true,
        result: {
          id: 'mock-user-id',
          email: 'test@example.com',
          name: 'Test User',
          profileImageUrl: 'https://picsum.photos/200',
        },
      },
    }).as('loginRequest');

    cy.intercept('GET', '**/public/api/v1.0/users', {
      statusCode: 200,
      body: {
        success: true,
        result: {
          id: 'mock-user-id',
          email: 'test@example.com',
          name: 'Test User',
          profileImageUrl: 'https://picsum.photos/200',
        },
      },
    }).as('getUser');
  });
  
  it('should navigate to login page and sign in successfully', () => {
    cy.visit('/');

    cy.contains('Sign In').click();
    cy.url().should('include', '/signin');

    cy.get('input[name="email"]').type('test@example.com');
    cy.get('input[name="password"]').type('password123');

    cy.get('button[type="submit"]').click();

    cy.wait('@loginRequest');

    cy.url().should('eq', Cypress.config().baseUrl + '/');
    
    cy.contains('Sign In').should('not.exist');
  });
});
