/*
 * Copyright 2018 The StartupOS Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.startupos.tools.aa.commands;

import com.google.startupos.tools.reviewer.service.CodeReviewServiceGrpc;
import com.google.startupos.tools.reviewer.service.Protos.CreateDiffRequest;
import com.google.startupos.tools.reviewer.service.Protos.Diff;
import com.google.startupos.tools.reviewer.service.Protos.Diff.Status;
import com.google.startupos.tools.reviewer.service.Protos.DiffRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import javax.inject.Inject;
import javax.inject.Named;

public class ReviewCommand implements AaCommand {
  private static final Integer GRPC_PORT = 8001;

  private final CodeReviewServiceGrpc.CodeReviewServiceBlockingStub codeReviewBlockingStub;
  private final Integer currentDiffNumber;

  @Inject
  public ReviewCommand(@Named("Current diff number") Integer currentDiffNumber) {
    this.currentDiffNumber = currentDiffNumber;
    ManagedChannel channel =
        ManagedChannelBuilder.forAddress("localhost", GRPC_PORT).usePlaintext().build();
    codeReviewBlockingStub = CodeReviewServiceGrpc.newBlockingStub(channel);
  }

  @Override
  public boolean run(String[] args) {
    if (currentDiffNumber == -1) {
      System.out.println(
          RED_ERROR + "Workspace has no diff to review (git branch has no D# branch)");
      return false;
    }
    Diff.Builder diffBuilder =
        codeReviewBlockingStub
            .getDiff(DiffRequest.newBuilder().setDiffId(currentDiffNumber).build())
            .toBuilder();
    if (diffBuilder.getReviewerCount() == 0) {
      System.out.println(String.format("D%d has no reviewers", currentDiffNumber));
      return false;
    }
    for (int i = 0; i < diffBuilder.getReviewerCount(); i++) {
      diffBuilder.setReviewer(i, diffBuilder.getReviewer(i).toBuilder().setNeedsAttention(true));
    }
    diffBuilder.setAuthor(diffBuilder.getAuthor().toBuilder().setNeedsAttention(false));
    diffBuilder.setStatus(Status.UNDER_REVIEW).build();
    codeReviewBlockingStub.createDiff(
        CreateDiffRequest.newBuilder().setDiff(diffBuilder.build()).build());
    return true;
  }
}

