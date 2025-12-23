
describe('Memo Flow', () => {
  beforeEach(() => {
    cy.setCookie('refreshToken', 'mock-restored-access-token');

    cy.intercept('POST', '**/api/v1.0/users/refresh', {
      statusCode: 200,
      headers: {
        authorization: 'Bearer mock-restored-access-token',
      },
      body: {
        success: true,
        result: null
      }
    }).as('refreshToken');

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

    cy.intercept('GET', '**/api/v1.0/schemas/*/memos', {
      statusCode: 200,
      body: {
        success: true,
        result: []
      }
    }).as('getMemos');

    const memoContent = 'Memo Flow Test Content';
    cy.intercept('POST', '**/api/v1.0/schemas/*/memos', {
      statusCode: 201,
      body: {
        success: true,
        result: {
          id: 'new-memo-id',
          content: memoContent,
          x: 400,
          y: 300,
          width: 200,
          height: 100,
          styles: {},
        }
      }
    }).as('createMemo');
  });

  it('should load canvas and create a new memo', () => {
    cy.visit('/canvas');

    cy.wait('@refreshToken');

    cy.get('.react-flow__pane', { timeout: 10000 }).should('be.visible');

    cy.get('div.absolute.bottom-4.left-1\\/2').find('button').eq(4).click();

    cy.get('.react-flow__pane').click(400, 300);

    const memoContent = 'Memo Flow Test Content';

    cy.get('input[type="text"]').type(memoContent);

    cy.get('.w-8').click();

    cy.wait('@createMemo').its('request.body').should((body) => {
        expect(body).to.include({ content: memoContent });
    });
    cy.get('.react-flow__node-memo').should('exist').and('contain.text', memoContent);
  });
});
