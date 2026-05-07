package com.waterlevel.server.service;

import com.waterlevel.server.dto.LoginRequest;
import com.waterlevel.server.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
