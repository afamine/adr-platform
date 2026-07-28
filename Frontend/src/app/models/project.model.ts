export interface ProjectDto {
  id: string;
  name: string;
  description?: string | null;
  archived: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectRequest {
  name: string;
  description?: string | null;
}
