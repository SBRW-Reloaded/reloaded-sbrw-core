/*
 * This file is part of the Soapbox Race World core source code.
 * If you use any of this code for third-party purposes, please provide attribution.
 * Copyright (c) 2020.
 */

package com.soapboxrace.core.api;

import com.soapboxrace.core.api.util.Secured;
import com.soapboxrace.core.bo.RequestSessionInfo;
import com.soapboxrace.core.bo.SocialRelationshipBO;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/addfriendrequest")
public class AddFriendRequest {

    @EJB
    private SocialRelationshipBO socialRelationshipBO;

    @Inject
    private RequestSessionInfo requestSessionInfo;

    @GET
    @Secured
    @Produces(MediaType.APPLICATION_XML)
    public Response addFriendRequest(@HeaderParam("securityToken") String securityToken,
                                     @QueryParam("displayName") String displayName) {
        return Response.ok().entity(socialRelationshipBO.addFriend(
                requestSessionInfo.getActivePersonaId(), displayName)).build();
    }
}