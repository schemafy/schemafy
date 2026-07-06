import { useEffect, useState } from 'react';
import { cn, toCapitalized } from '@/lib';
import { Button, QueryStateBoundary } from '@/components';
import {
  ConfirmDialog,
  InviteDialog,
  SharedProjectsTab,
  WorkspaceFormDialog,
  WorkspaceMembersTab,
  WorkspaceProjectsTab,
  WorkspaceSidebar,
} from '@/features/workspace/components';
import { useWorkspace } from '@/features/workspace/hooks/useWorkspace';
import { useWorkspaces } from '@/features/workspace/hooks/useWorkspaces';

type TabType = 'projects' | 'members';

const MY_PROJECTS_ID = '__my_projects__';

export const WorkspacePage = () => {
  const [selectedWorkspaceId, setSelectedWorkspaceId] = useState('');
  const [activeTab, setActiveTab] = useState<TabType>('projects');
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [isInviteDialogOpen, setIsInviteDialogOpen] = useState(false);
  const [isLeaveConfirmOpen, setIsLeaveConfirmOpen] = useState(false);

  const {
    workspacesData,
    workspaces,
    isPendingWorkspaces,
    isWorkspacesError,
    refetchWorkspaces,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
    leaveWorkspace,
  } = useWorkspaces();
  const isMyProjectsSelected = selectedWorkspaceId === MY_PROJECTS_ID;
  const { workspace: selectedWorkspace } = useWorkspace(
    isMyProjectsSelected ? '' : selectedWorkspaceId,
  );

  const currentUserRole =
    selectedWorkspace?.currentUserRole.toUpperCase() ?? '';

  useEffect(() => {
    if (!workspacesData || isMyProjectsSelected) return;

    if (workspaces.length > 0) {
      const isInList = workspaces.some((w) => w.id === selectedWorkspaceId);
      if (!selectedWorkspaceId || !isInList) {
        setSelectedWorkspaceId(workspaces[0].id);
      }
      return;
    }

    setSelectedWorkspaceId(MY_PROJECTS_ID);
  }, [isMyProjectsSelected, selectedWorkspaceId, workspaces, workspacesData]);

  const handleWorkspaceSelect = (id: string) => {
    setSelectedWorkspaceId(id);
    setActiveTab('projects');
  };

  const tabs: { key: TabType; label: string }[] = [
    { key: 'projects', label: 'Projects' },
    { key: 'members', label: 'Members' },
  ];

  return (
    <QueryStateBoundary
      data={workspacesData}
      isPending={isPendingWorkspaces}
      isError={isWorkspacesError}
      onRetry={() => void refetchWorkspaces()}
    >
      {(loadedWorkspacesData) => (
        <div className="flex min-h-full w-full flex-col overflow-x-hidden md:flex-row md:overflow-hidden">
          <WorkspaceSidebar
            pinnedItem={{
              id: MY_PROJECTS_ID,
              name: 'Invited Projects',
              description: 'Invited Projects',
            }}
            workspaces={workspaces}
            selectedId={
              selectedWorkspaceId ||
              (loadedWorkspacesData.totalElements === 0 ? MY_PROJECTS_ID : '')
            }
            onSelect={handleWorkspaceSelect}
            onAdd={() => setIsCreateDialogOpen(true)}
            isOpen={isSidebarOpen}
            onToggle={() => setIsSidebarOpen((prev) => !prev)}
            hasNextPage={hasNextPage}
            isFetchingNextPage={isFetchingNextPage}
            onLoadMore={() => void fetchNextPage()}
          />

          {isMyProjectsSelected || loadedWorkspacesData.totalElements === 0 ? (
            <div className="flex-1 flex justify-center py-6 px-8 overflow-hidden">
              <div
                className={cn(
                  'w-full max-w-2xl flex flex-col gap-6 transition-transform duration-300',
                  isSidebarOpen ? '-translate-x-32' : '-translate-x-6',
                )}
              >
                <div className="flex items-center justify-between">
                  <h1 className="font-heading-xl text-schemafy-text">
                    Invited Projects
                  </h1>
                </div>

                <SharedProjectsTab />
              </div>
            </div>
          ) : (
            <div className="flex min-w-0 flex-1 justify-center overflow-x-hidden px-4 py-6 sm:px-6 lg:px-10">
              <div className="flex w-full max-w-6xl flex-col gap-5 transition-transform duration-300">
                <section className="border-b border-schemafy-glass-border/70 pb-5">
                  <div className="flex flex-col gap-5">
                    <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                      <div className="min-w-0">
                        <div className="flex min-w-0 flex-wrap items-center gap-2">
                          <h1 className="truncate font-heading-xl text-schemafy-text">
                            {selectedWorkspace?.name}
                          </h1>
                          {currentUserRole && (
                            <span className="schemafy-badge px-2.5 py-1 font-caption-md">
                              {toCapitalized(currentUserRole)}
                            </span>
                          )}
                        </div>
                        <p className="mt-2 max-w-2xl font-body-sm text-schemafy-dark-gray">
                          {selectedWorkspace?.description ||
                            'Manage projects, members, and schema work in one workspace.'}
                        </p>
                      </div>
                      {currentUserRole === 'ADMIN' && (
                        <div className="flex shrink-0 items-center gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            className="h-9 px-4"
                            onClick={() => setIsEditDialogOpen(true)}
                          >
                            Edit
                          </Button>
                          <Button
                            size="sm"
                            className="h-9 px-4"
                            onClick={() => setIsInviteDialogOpen(true)}
                          >
                            Invite
                          </Button>
                        </div>
                      )}
                    </div>

                    <div className="flex w-full gap-6 border-b border-schemafy-glass-border/60 sm:w-fit">
                      {tabs.map((tab) => (
                        <button
                          key={tab.key}
                          onClick={() => setActiveTab(tab.key)}
                          className={cn(
                            'schemafy-focus-ring -mb-px flex flex-1 items-center justify-center border-b-2 px-0 pb-2 font-overline-sm transition-colors sm:flex-none',
                            activeTab === tab.key
                              ? 'border-schemafy-soft-blue text-schemafy-text'
                              : 'border-transparent text-schemafy-dark-gray hover:text-schemafy-text',
                          )}
                        >
                          {tab.label}
                        </button>
                      ))}
                    </div>
                  </div>
                </section>

                <section>
                  {activeTab === 'projects' ? (
                    <WorkspaceProjectsTab
                      key={selectedWorkspaceId}
                      workspaceId={selectedWorkspaceId}
                      currentUserRole={currentUserRole}
                    />
                  ) : (
                    <WorkspaceMembersTab
                      workspaceId={selectedWorkspaceId}
                      currentUserRole={currentUserRole}
                    />
                  )}
                </section>

                <LeaveWarningComponent
                  onLeave={() => setIsLeaveConfirmOpen(true)}
                />
              </div>
            </div>
          )}
          <WorkspaceFormDialog
            open={isCreateDialogOpen}
            onOpenChange={setIsCreateDialogOpen}
            mode="create"
          />

          {!isMyProjectsSelected && loadedWorkspacesData.totalElements > 0 && (
            <>
              <WorkspaceFormDialog
                open={isEditDialogOpen}
                onOpenChange={setIsEditDialogOpen}
                mode="edit"
                workspaceId={selectedWorkspaceId}
                initialName={selectedWorkspace?.name ?? ''}
                initialDescription={selectedWorkspace?.description ?? ''}
              />

              <InviteDialog
                open={isInviteDialogOpen}
                onOpenChange={setIsInviteDialogOpen}
                workspaceId={selectedWorkspaceId}
                currentUserRole={currentUserRole}
              />

              <ConfirmDialog
                open={isLeaveConfirmOpen}
                onOpenChange={setIsLeaveConfirmOpen}
                title="Leave Workspace"
                description="If you leave the workspace, you will lose access to all projects and resources. Are you sure you want to leave?"
                confirmLabel="Leave"
                onConfirm={() => leaveWorkspace(selectedWorkspaceId)}
              />
            </>
          )}
        </div>
      )}
    </QueryStateBoundary>
  );
};

const LeaveWarningComponent = ({ onLeave }: { onLeave: () => void }) => {
  return (
    <div className="flex flex-col gap-4 border-t border-schemafy-glass-border/70 pt-5 sm:flex-row sm:items-center sm:justify-between sm:gap-6">
      <div className="flex flex-col gap-1">
        <span className="font-heading-xs text-schemafy-destructive">
          Leave Workspace
        </span>
        <span className="font-body-sm text-schemafy-dark-gray">
          If you leave the workspace, you will lose access to all projects and
          resources.
        </span>
      </div>
      <Button
        variant="destructive"
        size="sm"
        className="h-9 w-full shrink-0 px-4 sm:w-auto"
        onClick={onLeave}
      >
        Leave
      </Button>
    </div>
  );
};
