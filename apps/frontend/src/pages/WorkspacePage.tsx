import { useState } from 'react';
import { cn } from '@/lib';
import { Button } from '@/components';
import type { WorkspaceItem } from '@/components/Workspace';
import { WorkspaceMembersTab, WorkspaceProjectsTab, WorkspaceSidebar, } from '@/components/Workspace';

type TabType = 'projects' | 'members';

const MOCK_WORKSPACES: (WorkspaceItem & {
  projectCount: number;
})[] = [
  {
    id: '1',
    name: 'My Workspace',
    memberCount: 4,
    role: 'admin',
    projectCount: 5,
  },
  {
    id: '2',
    name: 'Team Alpha',
    memberCount: 7,
    role: 'editor',
    projectCount: 3,
  },
];

export const WorkspacePage = () => {
  const [selectedWorkspaceId, setSelectedWorkspaceId] = useState('1');
  const [activeTab, setActiveTab] = useState<TabType>('projects');
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);

  const selectedWorkspace = MOCK_WORKSPACES.find(
    (w) => w.id === selectedWorkspaceId,
  );

  const handleWorkspaceSelect = (id: string) => {
    setSelectedWorkspaceId(id);
    setActiveTab('projects');
  };

  const tabs: { key: TabType; label: string; count: number }[] = [
    {
      key: 'projects',
      label: 'Projects',
      count: selectedWorkspace?.projectCount ?? 0,
    },
    {
      key: 'members',
      label: 'Members',
      count: selectedWorkspace?.memberCount ?? 0,
    },
  ];

  return (
    <div className="flex w-full min-h-full">
      <WorkspaceSidebar
        workspaces={MOCK_WORKSPACES}
        selectedId={selectedWorkspaceId}
        onSelect={handleWorkspaceSelect}
        isOpen={isSidebarOpen}
        onToggle={() => setIsSidebarOpen((prev) => !prev)}
      />

      <div className="flex-1 flex justify-center py-6 px-8 overflow-hidden">
        <div
          className={cn(
            'w-full max-w-2xl flex flex-col gap-6 transition-[margin] duration-300',
            isSidebarOpen ? '-ml-32' : '-ml-6',
          )}
        >
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <h1 className="font-heading-xl text-schemafy-text">
                {selectedWorkspace?.name}
              </h1>
              <span className="px-3 py-1 bg-schemafy-button-bg text-schemafy-button-text font-caption-md rounded-full">
              {selectedWorkspace?.role}
            </span>
            </div>
            <div className="flex items-center gap-2">
              <Button variant="outline" size="sm">
                Edit
              </Button>
              <Button size="sm">
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
                <span className="font-caption-md">{tab.count}</span>
              </button>
            ))}
          </div>

          <div className="flex-1">
            {activeTab === 'projects' ? (
              <WorkspaceProjectsTab/>
            ) : (
              <WorkspaceMembersTab/>
            )}
          </div>

          {selectedWorkspace?.role === 'admin' ? (
            <div className="border border-schemafy-destructive/30 bg-schemafy-destructive/10 rounded-[12px] px-6 py-5 flex items-center justify-between gap-6">
              <div className="flex flex-col gap-1">
                <span className="font-heading-xs text-schemafy-destructive">Delete Workspace</span>
                <span className="font-body-sm text-schemafy-dark-gray">
                  워크스페이스를 삭제하면 모든 프로젝트와 멤버 데이터가 영구적으로 제거됩니다. 이 작업은 되돌릴 수 없습니다.
                </span>
              </div>
              <Button variant="destructive" size="sm" className="shrink-0">
                Delete
              </Button>
            </div>
          ) : (
            <div className="border border-schemafy-yellow/30 bg-schemafy-yellow/10 rounded-[12px] px-6 py-5 flex items-center justify-between gap-6">
              <div className="flex flex-col gap-1">
                <span className="font-heading-xs text-schemafy-yellow">Leave Workspace</span>
                <span className="font-body-sm text-schemafy-dark-gray">
                  워크스페이스를 나가면 모든 프로젝트와 리소스에 대한 접근 권한을 잃게 됩니다.
                </span>
              </div>
              <Button variant="none" size="sm" className="shrink-0 bg-schemafy-yellow text-white">
                Leave
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};