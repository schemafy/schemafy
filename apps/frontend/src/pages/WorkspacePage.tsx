import { useEffect, useState } from 'react';
import { cn } from '@/lib';
import { Button } from '@/components';
import {
  InviteDialog,
  WorkspaceFormDialog,
  WorkspaceMembersTab,
  WorkspaceProjectsTab,
  WorkspaceSidebar,
} from '@/features/workspace/components';
import {
  useGetMembers,
  useGetWorkspace,
  useGetWorkspaces,
  useLeaveWorkspace,
} from '@/features/workspace/hooks/useWorkspaces';

type TabType = 'projects' | 'members';

export const WorkspacePage = () => {
  const [selectedWorkspaceId, setSelectedWorkspaceId] = useState('');
  const [activeTab, setActiveTab] = useState<TabType>('projects');
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [isInviteDialogOpen, setIsInviteDialogOpen] = useState(false);

  const {data: workspacesData} = useGetWorkspaces();
  const {data: selectedWorkspace} = useGetWorkspace(selectedWorkspaceId);
  // const {mutate: deleteWorkspace} = useDeleteWorkspace();
  const {mutate: leaveWorkspace} = useLeaveWorkspace();

  const {data: membersData} = useGetMembers(selectedWorkspaceId, 0);

  const workspaces = workspacesData?.content ?? [];
  const currentUserRole = selectedWorkspace?.currentUserRole.toUpperCase() ?? '';

  useEffect(() => {
    if (workspaces.length > 0 && !selectedWorkspaceId) {
      setSelectedWorkspaceId(workspaces[0].id);
    }
  }, [workspaces, selectedWorkspaceId]);

  const handleWorkspaceSelect = (id: string) => {
    setSelectedWorkspaceId(id);
    setActiveTab('projects');
  };

  const tabs: { key: TabType; label: string; count?: number }[] = [
    {key: 'projects', label: 'Projects', count: 0},
    {key: 'members', label: 'Members', count: membersData?.totalElements},
  ];

  return (
    <div className="flex w-full min-h-full">
      <WorkspaceSidebar
        workspaces={workspaces}
        selectedId={selectedWorkspaceId}
        onSelect={handleWorkspaceSelect}
        onAdd={() => setIsCreateDialogOpen(true)}
        isOpen={isSidebarOpen}
        onToggle={() => setIsSidebarOpen((prev) => !prev)}
      />

      {workspacesData?.totalElements === 0 ?
        <div className="flex w-full min-h-full justify-center items-center">
          워크스페이스를 추가해 주세요
        </div> :
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
                <span
                  className="px-3 py-1 bg-schemafy-button-bg text-schemafy-button-text font-caption-md rounded-full">
                {currentUserRole}
              </span>
              </div>
              <div className="flex items-center gap-2">
                <Button variant="outline" size="sm" onClick={() => setIsEditDialogOpen(true)}>
                  Edit
                </Button>
                <Button size="sm" onClick={() => setIsInviteDialogOpen(true)}>
                  Invite
                </Button>
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
                  <span>{tab.count}</span>
                </button>
              ))}
            </div>

            <div className="flex-1">
              {activeTab === 'projects' ? (
                <WorkspaceProjectsTab/>
              ) : (
                <WorkspaceMembersTab
                  workspaceId={selectedWorkspaceId}
                  currentUserRole={currentUserRole}
                />
              )}
            </div>

            {/*<div*/}
            {/*  className="border border-schemafy-destructive/30 bg-schemafy-destructive/10 rounded-[12px] px-6 py-5 flex items-center justify-between gap-6">*/}
            {/*  <div className="flex flex-col gap-1">*/}
            {/*    <span className="font-heading-xs text-schemafy-destructive">*/}
            {/*      Delete Workspace*/}
            {/*    </span>*/}
            {/*    <span className="font-body-sm text-schemafy-dark-gray">*/}
            {/*      워크스페이스를 삭제하면 모든 프로젝트와 멤버 데이터가 영구적으로 제거됩니다. 이 작업은 되돌릴 수 없습니다.*/}
            {/*    </span>*/}
            {/*  </div>*/}
            {/*  <Button*/}
            {/*    variant="destructive"*/}
            {/*    size="sm"*/}
            {/*    className="shrink-0"*/}
            {/*    onClick={() => deleteWorkspace(selectedWorkspaceId)}*/}
            {/*  >*/}
            {/*    Delete*/}
            {/*  </Button>*/}
            {/*</div>*/}
            {currentUserRole && (
              <div
                className="border border-schemafy-yellow/30 bg-schemafy-yellow/10 rounded-[12px] px-6 py-5 flex items-center justify-between gap-6">
                <div className="flex flex-col gap-1">
                <span className="font-heading-xs text-schemafy-yellow">
                  Leave Workspace
                </span>
                  <span className="font-body-sm text-schemafy-dark-gray">
                  워크스페이스를 나가면 모든 프로젝트와 리소스에 대한 접근 권한을 잃게 됩니다.
                </span>
                </div>
                <Button
                  variant="none"
                  size="sm"
                  className="shrink-0 bg-schemafy-yellow text-white"
                  onClick={() => leaveWorkspace(selectedWorkspaceId)}
                >
                  Leave
                </Button>
              </div>
            )}
          </div>
        </div>
      }
      <WorkspaceFormDialog
        open={isCreateDialogOpen}
        onOpenChange={setIsCreateDialogOpen}
        mode="create"
      />

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
    </div>
  );
};
