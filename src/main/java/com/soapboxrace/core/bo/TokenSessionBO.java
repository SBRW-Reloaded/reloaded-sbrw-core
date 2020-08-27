/*
 * This file is part of the Soapbox Race World core source code.
 * If you use any of this code for third-party purposes, please provide attribution.
 * Copyright (c) 2020.
 */

package com.soapboxrace.core.bo;

import com.soapboxrace.core.dao.TokenSessionDAO;
import com.soapboxrace.core.dao.UserDAO;
import com.soapboxrace.core.engine.EngineException;
import com.soapboxrace.core.engine.EngineExceptionCode;
import com.soapboxrace.core.jpa.BanEntity;
import com.soapboxrace.core.jpa.TokenSessionEntity;
import com.soapboxrace.core.jpa.UserEntity;
import com.soapboxrace.jaxb.login.LoginStatusVO;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.UUID;

@Stateless
public class TokenSessionBO {
    @EJB
    private TokenSessionDAO tokenDAO;

    @EJB
    private UserDAO userDAO;

    @EJB
    private ParameterBO parameterBO;

    @EJB
    private GetServerInformationBO serverInfoBO;

    @EJB
    private AuthenticationBO authenticationBO;

    public String createToken(Long userId, String clientHostName) {
        TokenSessionEntity tokenSessionEntity = new TokenSessionEntity();
        Date expirationDate = getMinutes(parameterBO.getIntParam("SESSION_LENGTH_MINUTES", 130));
        tokenSessionEntity.setExpirationDate(expirationDate);
        String randomUUID = UUID.randomUUID().toString();
        tokenSessionEntity.setSecurityToken(randomUUID);
        UserEntity userEntity = userDAO.findById(userId);
        tokenSessionEntity.setUserEntity(userEntity);
        tokenSessionEntity.setPremium(userEntity.isPremium());
        tokenSessionEntity.setClientHostIp(clientHostName);
        tokenSessionEntity.setActivePersonaId(0L);
        tokenSessionEntity.setEventSessionId(null);
        tokenDAO.insert(tokenSessionEntity);
        return randomUUID;
    }

    @Deprecated
    public void verifyPersonaOwnership(String securityToken, Long personaId) {
        TokenSessionEntity tokenSession = tokenDAO.findById(securityToken);
        if (tokenSession == null) {
            throw new EngineException(EngineExceptionCode.NoSuchSessionInSessionStore, true);
        }

        if (!tokenSession.getUserEntity().ownsPersona(personaId)) {
            throw new EngineException(EngineExceptionCode.RemotePersonaDoesNotBelongToUser, true);
        }
    }

    public void deleteByUserId(Long userId) {
        tokenDAO.deleteByUserId(userId);
    }

    private Date getMinutes(int minutes) {
        long time = new Date().getTime();
        time = time + (minutes * 60000);
        return new Date(time);
    }

    public LoginStatusVO login(String email, String password, HttpServletRequest httpRequest) {
        LoginStatusVO loginStatusVO = new LoginStatusVO(0L, "", false);

        if (email != null && !email.isEmpty() && password != null && !password.isEmpty()) {
            UserEntity userEntity = userDAO.findByEmail(email);
            if (userEntity != null) {
                if (password.equals(userEntity.getPassword())) {
                    if (userEntity.isLocked()) {
                        loginStatusVO.setDescription("Account locked. Contact an administrator.");
                        return loginStatusVO;
                    }

                    BanEntity banEntity = authenticationBO.checkUserBan(userEntity);

                    if (banEntity != null) {
                        LoginStatusVO.Ban ban = new LoginStatusVO.Ban();
                        ban.setReason(banEntity.getReason());
                        if (banEntity.getEndsAt() != null)
                            ban.setExpires(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withZone(ZoneId.systemDefault()).format(banEntity.getEndsAt()));
                        loginStatusVO.setBan(ban);
                        return loginStatusVO;
                    }

                    userEntity.setLastLogin(LocalDateTime.now());
                    userDAO.update(userEntity);
                    Long userId = userEntity.getId();
                    deleteByUserId(userId);
                    String randomUUID = createToken(userId, httpRequest.getRemoteHost());
                    loginStatusVO = new LoginStatusVO(userId, randomUUID, true);
                    loginStatusVO.setDescription("");

                    return loginStatusVO;
                }
            }
        }
        loginStatusVO.setDescription("LOGIN ERROR");
        return loginStatusVO;
    }

    @Deprecated
    public Long getActivePersonaId(String securityToken) {
        TokenSessionEntity tokenSessionEntity = tokenDAO.findById(securityToken);
        return tokenSessionEntity.getActivePersonaId();
    }

    @Deprecated
    public void setActivePersonaId(String securityToken, Long personaId, Boolean isLogout) {
        TokenSessionEntity tokenSessionEntity = tokenDAO.findById(securityToken);

        if (!isLogout) {
            if (!tokenSessionEntity.getUserEntity().ownsPersona(personaId)) {
                throw new EngineException(EngineExceptionCode.RemotePersonaDoesNotBelongToUser, true);
            }
        }

        tokenSessionEntity.setActivePersonaId(personaId);
        tokenDAO.update(tokenSessionEntity);
    }

    @Deprecated
    public String getActiveRelayCryptoTicket(String securityToken) {
        TokenSessionEntity tokenSessionEntity = tokenDAO.findById(securityToken);
        return tokenSessionEntity.getRelayCryptoTicket();
    }

    @Deprecated
    public Long getActiveLobbyId(String securityToken) {
        TokenSessionEntity tokenSessionEntity = tokenDAO.findById(securityToken);
        return tokenSessionEntity.getActiveLobbyId();
    }

    @Deprecated
    public void setActiveLobbyId(String securityToken, Long lobbyId) {
        TokenSessionEntity tokenSessionEntity = tokenDAO.findById(securityToken);
        tokenSessionEntity.setActiveLobbyId(lobbyId);
        tokenDAO.update(tokenSessionEntity);
    }

    @Deprecated
    public boolean isAdmin(String securityToken) {
        return getUser(securityToken).isAdmin();
    }

    @Deprecated
    public UserEntity getUser(String securityToken) {
        return tokenDAO.findById(securityToken).getUserEntity();
    }

    @Deprecated
    public void setEventSessionId(String securityToken, Long eventSessionId) {
        TokenSessionEntity tokenSessionEntity = tokenDAO.findById(securityToken);
        tokenSessionEntity.setEventSessionId(eventSessionId);
        tokenDAO.update(tokenSessionEntity);
    }

    @Deprecated
    public Long getEventSessionId(String securityToken) {
        return tokenDAO.findById(securityToken).getEventSessionId();
    }
}