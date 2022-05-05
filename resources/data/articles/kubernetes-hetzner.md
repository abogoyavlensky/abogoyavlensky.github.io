This is just a quick note about provisioning a self-hosted Kubernetes cluster on Hetzner.
For a large production, I would suggest using any managed Kubernetes for example EKS, GKE, or DigitalOcean Kubernetes, you name it.
But sometimes you need a small cluster for personal projects or medium apps. 
And for that case managed solutions could be a bit expensive. 

So recently I faced this kind of problem and decided to figure out a way how to 
bootstrap self-hosted Kubernetes cluster but do not spend a lot of time 
to dealing with countless Kubernetes internal parts. 
Also, I would like to use Terraform as much as possible to avoid the manual configuration of the cluster.
I would like to use dynamically attached volumes to pods using CSI or similar to support statefull deployments. 
Communication inside the cluster should be by a private network.
It should be possible to extend the cluster by adding extra worker nodes.
In other words, the less manual support cluster would need the better.
For hosting, I chose [Hetzner](https://www.hetzner.com/cloud) as probably the cheapest and one of the most robust.

There are a lot of options to run self-hosted cluster:
- [kubespary](https://github.com/kubernetes-sigs/kubespray)
- [kops](https://github.com/kubernetes/kops)
- [k3s](https://k3s.io/)
- [k0s](https://k0sproject.io/)
- [kubeone](https://github.com/kubermatic/kubeone)
- ...


Finally, I end up with combination: [k3s](https://k3s.io/) + [kube-hetzner](https://github.com/kube-hetzner/terraform-hcloud-kube-hetzner) Terraform module. Because it just works for me and offers everythinng I need right from the box.
And in this note, I would like to share and discuss my setup in [the github repo](https://github.com/abogoyavlensky/k3s-provision/tree/0.1.0-article).

### Setup

All you need for installing a new cluster on fresh Hetzner project space is:
- clone the repo: `git clone git@github.com:abogoyavlensky/k3s-provision.git` or just copy the [`main.tf`](https://github.com/abogoyavlensky/k3s-provision/blob/0.1.0-article/main.tf) file;
- create the file [`env.auto.tfvars`](https://github.com/abogoyavlensky/k3s-provision/blob/0.1.0-article/env.auto.tfvars.example) at the root of the repo directory with two vars `hcloud_token` and `traefik_acme_email`; (*This file must not be committed!*)
- run `terraform init` and `terraform apply`.

After confirmation Terraform starts to install cluster which contains:
- one load-balancer node;
- one control-plane node;
- one worker node.

**TIP:** *it costs about ~16 Euro. It is possible to reduce the cost, even more, by using a single control-plane node with workload without an external load-balancer.*

The kube-hetzner module allows us to install the HA cluster, but right now, I don't need that, so I decided to reduce the cost.
Please check config in the [main terraform file](https://github.com/abogoyavlensky/k3s-provision/blob/0.1.0-article/main.tf) in the repo.
Every possible config and some doc you could find in [the official example](https://github.com/kube-hetzner/terraform-hcloud-kube-hetzner/blob/master/terraform.tfvars.example). There is the ability to increase worker and control plane nodes on the go. 
I [disabled](https://github.com/abogoyavlensky/k3s-provision/blob/dcf817fe47f2457df50f2dbc3a9ceb8dbc413df7/main.tf#L56) auto-upgrade of the cluster, but it is possible to upgrade it with a little bit of [manual work](https://github.com/kube-hetzner/terraform-hcloud-kube-hetzner#individual-components-upgrade). 

### Example

For demonstration purposes of cluster abilities, there is [an example](https://github.com/abogoyavlensky/k3s-provision/blob/0.1.0-article/examples/nginx.yaml) Nginx service with dynamically attached volume and automated TLS.
But before deploying the service let's install Traefik middleware for redirecting http requests to https:

```shell
kubectl apply -f deploy/middlewares.yaml
```

And edit [host](https://github.com/abogoyavlensky/k3s-provision/blob/0.1.0-article/examples/nginx.yaml#L61) with your domain in yaml config to check the actual service with https.
Also you should configure DNS for your domain to load-balancer IP.

Then you could deploy the demo service:

```shell
kubectl apply -f examples/nginx.yaml
```

After a few seconds, you could check the `https://your.domain.com` and see the Nginx greeting page.

### Conclusion

That's how I ran my Kubernetes cluster for personal projects.
Of course, there is a room for improvement:
- using terraform tfstate backend for storing infra state somewhere externally;
- attaching floating IP to load-balancer;
- checking terraform files formatting and correctness in CI on every commit;
- increasing control planes at least up to 3 and worker nodes as needed;
- figuring out how to upgrade cluster internals.

The goal of the current note is just to give the simple approach to run a cluster in minutes with good practices in it and without manual work.
