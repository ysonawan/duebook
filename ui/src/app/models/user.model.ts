export interface UserProfile {
  id: number;
  name: string;
  email: string;
  secondaryEmails: string[];
}

export interface UpdateSecondaryEmailsRequest {
  secondaryEmails: string[];
}

