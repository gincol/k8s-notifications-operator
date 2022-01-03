package es.vn.operator;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSource;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.LocalObjectReferenceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;

@Controller(namespaces = "openshift-operators")
public class K8sNotificationsController implements ResourceController<K8sNotifications> {

	private final Logger log = LoggerFactory.getLogger(K8sNotificationsController.class);
	private final KubernetesClient kubernetesClient;

	@ConfigProperty(name = "pod.namespace")
	String podNamespace;

	@ConfigProperty(name = "pod.service-account")
	String podServiceAccount;

	@ConfigProperty(name = "pod.image-pull-secrets")
	String podImagePullSecrets;

	public K8sNotificationsController(KubernetesClient client) {
		this.kubernetesClient = client;
	}

	@Override
	public UpdateControl<K8sNotifications> createOrUpdateResource(K8sNotifications k8sNotificationsRequest,
			Context<K8sNotifications> context) {

		log.info("creando resources");
		ConfigMap configMap = createConfigMap(k8sNotificationsRequest);
		Secret secret = createSecret(k8sNotificationsRequest);
		createDeployment(k8sNotificationsRequest, configMap, secret);

		return UpdateControl.noUpdate();
	}
	
	@Override
	public DeleteControl deleteResource(K8sNotifications k8sNotificationsRequest, Context<K8sNotifications> context) {
		final var spec = k8sNotificationsRequest.getSpec();
		String namespace = spec.getNamespace();
		String name = spec.getName();
		String nameObjectDeployed = name.concat("-").concat(namespace);

		deleteCR(nameObjectDeployed);
		deleteDeployment(nameObjectDeployed);
		deleteConfigMap(nameObjectDeployed);
		deleteSecret(nameObjectDeployed);

		return DeleteControl.DEFAULT_DELETE;
	}

	private ConfigMap createConfigMap(K8sNotifications k8sNotificationsRequest) {
		final var spec = k8sNotificationsRequest.getSpec();
		String namespace = spec.getNamespace();
		String nameObjectToDeploy = spec.getName().concat("-").concat(namespace);

		//deleteConfigMap(nameObjectToDeploy);
		log.info(String.format("creando configMap %s", nameObjectToDeploy));

		// @formatter:off
		ConfigMap configMapBuild = new ConfigMapBuilder()
				.withNewMetadata().withName(nameObjectToDeploy).endMetadata()
				.addToData("TARGET_NAMESPACE", namespace)
				.addToData("SLACK_TOKEN", spec.getSlackToken())
				.addToData("SLACK_CHANNEL_ID", spec.getSlackChannelId())
				
				.addToData("DB_KIND", spec.getDatabase().getDbKind())
				.addToData("DB_URL", spec.getDatabase().getDbUrl())
				.addToData("DB_USERNAME", spec.getDatabase().getDbUsername())
				.addToData("DB_PASSWORD", spec.getDatabase().getDbPassword())
				.build();
		// @formatter:on
		
		ConfigMap configMap = kubernetesClient.configMaps().inNamespace(podNamespace).createOrReplace(configMapBuild);
		log.info(String.format("configMap creado... name: %s, namespace: %s", nameObjectToDeploy, podNamespace));
		return configMap;
	}

	private Secret createSecret(K8sNotifications k8sNotificationsRequest) {
		final var spec = k8sNotificationsRequest.getSpec();
		String namespace = spec.getNamespace();
		String nameObjectToDeploy = spec.getName().concat("-").concat(namespace);

		//deleteSecret(nameObjectToDeploy);
		log.info(String.format("creando secret %s", nameObjectToDeploy));
		
		// @formatter:off
		Secret secretBuild = new SecretBuilder()
				.withNewMetadata().withName(nameObjectToDeploy).endMetadata()
				.addToStringData("DB_KIND", spec.getDatabase().getDbKind())
				.addToStringData("DB_URL", spec.getDatabase().getDbUrl())
				.addToStringData("DB_USERNAME", spec.getDatabase().getDbUsername())
				.addToStringData("DB_PASSWORD", spec.getDatabase().getDbPassword())
				.build();
		// @formatter:on
		
		Secret secret = kubernetesClient.secrets().inNamespace(podNamespace).createOrReplace(secretBuild);
		log.info(String.format("secret creado... name: %s, namespace: %s", nameObjectToDeploy, podNamespace));
		return secret;
	}

	private void createDeployment(K8sNotifications k8sNotificationsRequest, ConfigMap configMap, Secret secret) {
		final var spec = k8sNotificationsRequest.getSpec();

		String image = spec.getImage();
		String namespace = spec.getNamespace();
		String nameObjectToDeploy = spec.getName().concat("-").concat(namespace);
		log.info(String.format("creando deployment %s", nameObjectToDeploy));

//		EnvFromSource envFromSource = new EnvFromSource(
//				new ConfigMapEnvSource(configMap.getMetadata().getName(), false), null,
//				new SecretEnvSource(secret.getMetadata().getName(), false));
		EnvFromSource envFromSource = new EnvFromSource(
				new ConfigMapEnvSource(configMap.getMetadata().getName(), false), null, null);

		Deployment deployment = kubernetesClient.apps().deployments().inNamespace(podNamespace)
				.withName(nameObjectToDeploy).get();
		if (deployment == null) {
			// @formatter:off
			deployment = new DeploymentBuilder()
				.withNewMetadata()
					.withName(nameObjectToDeploy)
					.addToLabels("app",nameObjectToDeploy)
				.endMetadata()
				.withNewSpec()
					.withReplicas(1)
					.withNewTemplate()
						.withNewMetadata()
						.addToLabels("app", nameObjectToDeploy)
						.endMetadata()
						.withNewSpec()
							.withServiceAccount(podServiceAccount)
							.withImagePullSecrets(new LocalObjectReferenceBuilder().withName(podImagePullSecrets).build())
							.addNewContainer()
								.withName(nameObjectToDeploy)
								.withImage(image)
								.withImagePullPolicy("Always")
								.addNewPort().withContainerPort(8080).endPort()
								.withEnvFrom(envFromSource)
							.endContainer()
						.endSpec()
					.endTemplate()
					.withNewSelector()
						.addToMatchLabels("app",nameObjectToDeploy)
					.endSelector()
				.endSpec()
			.build();
			// @formatter:on
			kubernetesClient.apps().deployments().inNamespace(podNamespace).create(deployment);
			log.info(String.format("deploy creado... name: %s, namespace: %s", nameObjectToDeploy, podNamespace));
		}
	}

	private void deleteCR(String nameObjectDeployed) {
		K8sNotifications k8sn = kubernetesClient.customResources(K8sNotifications.class).inNamespace(podNamespace)
				.withName(nameObjectDeployed).get();
		if (k8sn != null) {
			Boolean crDeleted = kubernetesClient.customResources(K8sNotifications.class).inNamespace(podNamespace)
					.delete();
			log.info(String.format("borrado cr... name: %s, namespace: %s, result: %s", nameObjectDeployed,
					podNamespace, crDeleted));
		}
	}

	private void deleteDeployment(String nameObjectDeployed) {
		Deployment deployment = kubernetesClient.apps().deployments().inNamespace(podNamespace)
				.withName(nameObjectDeployed).get();
		if (deployment != null) {
			Boolean deploymentDeleted = kubernetesClient.apps().deployments().inNamespace(podNamespace)
					.withName(nameObjectDeployed).delete();
			log.info(String.format("borrado deployment... name: %s, namespace: %s, result: %s", nameObjectDeployed,
					podNamespace, deploymentDeleted));
		}
	}

	private void deleteConfigMap(String nameObjectDeployed) {
		ConfigMap configMap = kubernetesClient.configMaps().inNamespace(podNamespace).withName(nameObjectDeployed)
				.get();
		if (configMap != null) {
			Boolean configMapDeleted = kubernetesClient.configMaps().inNamespace(podNamespace)
					.withName(nameObjectDeployed).delete();
			log.info(String.format("borrado configmap... name: %s, namespace: %s, result: %s", nameObjectDeployed,
					podNamespace, configMapDeleted));
		}
	}

	private void deleteSecret(String nameObjectDeployed) {
		Secret secret = kubernetesClient.secrets().inNamespace(podNamespace).withName(nameObjectDeployed).get();
		if (secret != null) {
			Boolean secretDeleted = kubernetesClient.secrets().inNamespace(podNamespace).withName(nameObjectDeployed)
					.delete();
			log.info(String.format("borrado secret... name: %s, namespace: %s, result: %s", nameObjectDeployed,
					podNamespace, secretDeleted));
		}
	}

	@Override
	public void init(EventSourceManager eventSourceManager) {

	}
}
