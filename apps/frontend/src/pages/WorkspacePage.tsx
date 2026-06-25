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
        <div className="flex w-full min-h-full">
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
            <div className="flex-1 flex justify-center py-6 px-8 overflow-hidden">
              <div
                className={cn(
                  'w-full max-w-2xl flex flex-col gap-6 transition-transform duration-300',
                  isSidebarOpen ? '-translate-x-32' : '-translate-x-6',
                )}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <h1 className="font-heading-xl text-schemafy-text">
                      {selectedWorkspace?.name}
                    </h1>
                    <span className="px-3 py-1 bg-schemafy-button-bg text-schemafy-button-text font-caption-md rounded-full">
                      {toCapitalized(currentUserRole)}
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    {currentUserRole === 'ADMIN' && (
                      <>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setIsEditDialogOpen(true)}
                        >
                          Edit
                        </Button>
                        <Button
                          size="sm"
                          onClick={() => setIsInviteDialogOpen(true)}
                        >
                          Invite
                        </Button>
                      </>
                    )}
                  </div>
                </div>

                <div className="flex gap-6 border-b border-schemafy-light-gray">
                  {tabs.map((tab) => (
                    <button
                      key={tab.key}
                      onClick={() => setActiveTab(tab.key)}
                      className={cn(
                        'pb-3 font-overline-sm flex items-center gap-1.5 border-b-2 -mb-px transition-colors',
                        activeTab === tab.key
                          ? 'border-schemafy-text text-schemafy-text'
                          : 'border-transparent text-schemafy-dark-gray hover:text-schemafy-text',
                      )}
                    >
                      {tab.label}
                    </button>
                  ))}
                </div>

                <div className="flex-1">
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
                </div>

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
    <div className="border border-schemafy-yellow/30 bg-schemafy-yellow/10 rounded-[12px] px-6 py-5 flex items-center justify-between gap-6">
      <div className="flex flex-col gap-1">
        <span className="font-heading-xs text-schemafy-yellow">
          Leave Workspace
        </span>
        <span className="font-body-sm text-schemafy-dark-gray">
          If you leave the workspace, you will lose access to all projects and
          resources.
        </span>
      </div>
      <Button
        variant="none"
        size="sm"
        className="shrink-0 bg-schemafy-yellow text-white"
        onClick={onLeave}
      >
        Leave
      </Button>
    </div>
  );
};
