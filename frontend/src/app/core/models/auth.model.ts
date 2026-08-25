export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginCpfRequest {
  cpf: string;
}

export interface ClientTokenResponse {
  token: string;
  tokenType: string;
  expiresIn: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
}
