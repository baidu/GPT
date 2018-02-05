/**
 * Copyright (c) 2014 Baidu, Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.content.pm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import android.content.IntentFilter;
import android.os.Bundle;
import android.util.DisplayMetrics;

/**
 * Hook系统的对应接口类方法
 * Package archive parsing
 *
 * @author liuhaitao
 * @since 2015-05-13
 */
public class PackageParser {

    public PackageParser() {

    }

    public PackageParser(String archiveSourcePath) {
    }

    public Package parsePackage(File packageFile, int flags) throws PackageParserException {
        return null;
    }

    public Package parsePackage(File sourceFile, String destCodePath, DisplayMetrics metrics, int flags) {
        return null;
    }

    public final static class Package {
        public final ArrayList<Activity> activities = new ArrayList<Activity>(0);
        public Bundle mAppMetaData = null;
        public final ArrayList<Activity> receivers = new ArrayList<Activity>(0);
        public final ArrayList<Provider> providers = new ArrayList<Provider>(0);
        public final ArrayList<Service> services = new ArrayList<Service>(0);
    }

    public static class Component<II extends IntentInfo> {
        public final ArrayList<II> intents = null;
        public final String className = null;
    }

    public final static class Activity extends Component<ActivityIntentInfo> {
        public Activity(Package _owner) {
        }

        public final ActivityInfo info = null;
    }

    public static class IntentInfo extends IntentFilter {
    }

    public final static class ActivityIntentInfo extends IntentInfo {
        public final Activity activity;

        public ActivityIntentInfo(Activity _activity) {
            activity = _activity;
        }
    }

    public final static class Service extends Component<ServiceIntentInfo> {
        public final ServiceInfo info = null;
    }

    public final static class ServiceIntentInfo extends IntentInfo {
        public final Service service;

        public ServiceIntentInfo(Service _service) {
            service = _service;
        }
    }

    public final static class Provider extends Component<ProviderIntentInfo> {
        public final ProviderInfo info = null;
    }

    public static final class ProviderIntentInfo extends IntentInfo {
        public final Provider provider;

        public ProviderIntentInfo(Provider provider) {
            this.provider = provider;
        }
    }

    public static class PackageParserException extends Exception {
    }

    public void collectCertificates(Package pkg, int flags) {

    }

    public void collectManifestDigest(Package pkg) {

    }

    /**
     * 2.3 4.0
     */
    public static PackageInfo generatePackageInfo(PackageParser.Package p, int gids[], int flags,
                                                  long firstInstallTime, long lastUpdateTime) {
        return null;
    }

    /**
     * 4.1
     */
    public static PackageInfo generatePackageInfo(PackageParser.Package p, int gids[], int flags,
                                                  long firstInstallTime, long lastUpdateTime, HashSet<String> grantedPermissions, boolean stopped,
                                                  int enabledState) {
        return null;
    }

    /*
    
    // 4.2 4.3 4.4 5.0 
    public static PackageInfo generatePackageInfo(PackageParser.Package p,
            int gids[], int flags, long firstInstallTime, long lastUpdateTime,
            HashSet<String> grantedPermissions, PackageUserState state) {
        return null;
    }
    
    // 5.1
    public static PackageInfo generatePackageInfo(PackageParser.Package p,
            int gids[], int flags, long firstInstallTime, long lastUpdateTime,
            ArraySet<String> grantedPermissions, PackageUserState state) {
        return null;
    }
    
    //6.0
    public static PackageInfo generatePackageInfo(PackageParser.Package p,
            int gids[], int flags, long firstInstallTime, long lastUpdateTime,
            Set<String> grantedPermissions, PackageUserState state)  {
        return null;
    }
*/
    public static final ActivityInfo generateActivityInfo(Activity a, int flags, PackageUserState state, int userId) {
        return null;
    }

    public static final ServiceInfo generateServiceInfo(Service s, int flags, PackageUserState state, int userId) {
        return null;
    }

    public static final ProviderInfo generateProviderInfo(Provider p, int flags, PackageUserState state, int userId) {
        return null;
    }
}
