// Copyright (C) 2020 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.github.oauth;

import org.junit.Assert;
import org.junit.Test;

import org.kohsuke.github.GHPersonSet;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import static org.hamcrest.Matchers.*;

public class BridgeMethodTest {

    @Test
    public void testBridgeMethods() throws IOException {
        verifyBridgeMethods(GitHubMyselfWrapper.class, "getFollows", GHPersonSet.class, Set.class);
        verifyBridgeMethods(GitHubMyselfWrapper.class, "getFollowers", GHPersonSet.class, Set.class);
        verifyBridgeMethods(GitHubMyselfWrapper.class, "getOrganizations", GHPersonSet.class, Set.class);
        verifyBridgeMethods(GitHubMyselfWrapper.class, "getId", int.class, long.class, String.class);

        verifyBridgeMethods(FooClass.class, "getId", int.class, long.class, String.class);
    }

    void verifyBridgeMethods(@Nonnull Class<?> targetClass, @Nonnull String methodName, Class<?>... returnTypes) {
        List<Class<?>> foundMethods = new ArrayList<>();
        Method[] methods = targetClass.getMethods();
        for (Method method : methods) {
            if (method.getName().equalsIgnoreCase(methodName)) {
                Assert.assertThat(method.getParameterCount(), equalTo(0));
                foundMethods.add(method.getReturnType());
            }
        }

        Assert.assertThat(foundMethods, containsInAnyOrder(returnTypes));
    }
}
