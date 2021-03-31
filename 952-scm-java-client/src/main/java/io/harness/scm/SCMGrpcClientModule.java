package io.harness.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.product.ci.scm.proto.SCMGrpc.newBlockingStub;

import io.harness.annotations.dev.OwnedBy;
import io.harness.govern.ProviderModule;
import io.harness.product.ci.scm.proto.SCMGrpc;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.grpc.Channel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import javax.net.ssl.SSLException;

@OwnedBy(DX)
public class SCMGrpcClientModule extends ProviderModule {
  io.harness.scm.ScmConnectionConfig scmConnectionConfig;

  public SCMGrpcClientModule(ScmConnectionConfig scmConnectionConfig) {
    this.scmConnectionConfig = scmConnectionConfig;
  }

  @Provides
  @Singleton
  SCMGrpc.SCMBlockingStub scmServiceBlockingStub() throws SSLException {
    return newBlockingStub(scmChannel(scmConnectionConfig.getUrl()));
  }

  @Singleton
  @Provides
  public Channel scmChannel(String connectionUrl) {
    // TODO: Authentication Needs to be added here.
    return NettyChannelBuilder.forTarget(connectionUrl).usePlaintext().build();
  }
}