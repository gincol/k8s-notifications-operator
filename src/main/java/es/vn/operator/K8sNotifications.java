package es.vn.operator;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("operators.vn.com")
@Version("v1alpha1")
@ShortNames("k8sn")
public class K8sNotifications extends CustomResource<K8sNotificationsSpec, K8sNotificationsStatus> implements Namespaced {
    private K8sNotificationsSpec spec;
    private K8sNotificationsStatus status;
}
