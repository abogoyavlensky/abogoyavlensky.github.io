This is just a quick note about provisioning a self-hosted Kubernetes cluster in Hetzner.
For a large production, I would suggest using any managed Kubernetes for example EKS, GKE, or DigitalOcean Kubernetes you name it.
But sometimes you need some small cluster for personal projects or medium apps. 
And for that case managed solutions could be a bit expensive. 

So recently I faced this kind of problem and decided to figure out a way how to 
bootstrap self-hosted Kubernetes cluster but do not spend a lot of time 
to dealing with countless Kubernetes internal parts. 
Also, I would like to use Terraform as much as possible to avoid the manual configuration of the cluster.
I would like to use dynamically attached volumes to pods using CSI or similar. 
In other words, the less support cluster would need in the future the better.
For hosting, I choose [Hetzner](https://www.hetzner.com/cloud) as probably the cheapest and one of the most robust ones.

Finally I end up with combination: [k3s](https://k3s.io/) + [kube-hetzner](https://github.com/kube-hetzner/terraform-hcloud-kube-hetzner/blob/master/terraform.tfvars.example) Terraform module.
And in this note, I would like to share my setup in [github repo](https://github.com/abogoyavlensky/k3s-provision).
Communication inside the cluster should be by a private network.
All you need for installing a new cluster on fresh Hetzner project space is:
- clone the repo: `git clone git@github.com:abogoyavlensky/k3s-provision.git`;
- create the file [`env.auto.tfvars`](https://github.com/abogoyavlensky/k3s-provision/blob/master/env.auto.tfvars.example) at the root of the repo directory with two vars `hcloud_token` and `traefik_acme_email`; (*This file must not be committed!*)
- and run `terraform apply`.

After confirmation Terraform starts to install cluster which contains:
- one load-balancer node;
- one control-plane node;
- one worker node.

*It costs about ~16 Euro. It is possible to reduce the cost, even more, by using a single control-plane node without an external load-balancer.*

The kube-hetzner module allows us to install the truly HA cluster but for personal projects, I don't need that so I decided to reduce the cost.
Please check config in the [main terraform file](https://github.com/abogoyavlensky/k3s-provision/blob/master/main.tf) in the repo.
Every possible config and some doc you could find in [the official example](https://github.com/kube-hetzner/terraform-hcloud-kube-hetzner/blob/master/terraform.tfvars.example). There is the ability to increase worker and control plane nodes on the go. 
I [disabled](https://github.com/abogoyavlensky/k3s-provision/blob/3fcbb9a6943b48dedb347c4f47e3f1af78e72b80/main.tf#L56) auto-upgrade Kubernetes, but it is possible to upgrade it with a little bit of [manual work](https://github.com/kube-hetzner/terraform-hcloud-kube-hetzner#individual-components-upgrade). 

For demonstration purposes of cluster abilities, there is [an example](https://github.com/abogoyavlensky/k3s-provision/blob/master/examples/nginx.yaml) Nginx service with dynamically attached volume.
But before deploying the service let's install Traefik middleware for redirecting http requests to https:

```shell
kubectl apply -f deploy/middlewares.yaml
```

And edit [host](https://github.com/abogoyavlensky/k3s-provision/blob/3fcbb9a6943b48dedb347c4f47e3f1af78e72b80/examples/nginx.yaml#L61) with your domain in Kubernetes yaml to check the actual service with https.

Then you could deploy the example service:

```shell
kubectl apply -f example/nginx.yaml
```

After a few seconds, you could check the `your.domain.com` and see the Nginx greeting page.

That's how I ran my Kubernetes cluster for personal projects.
Of course, there is a room for improvement:
- attaching floating to load-balancer;
- using terraform tfstate backend for not storing infra state locally;
- increasing control planes at least up to 3 and worker nodes as needed;
- checking terraform files formatting and correctness in CI on every commit.

The goal of the current note is just to give the way to run a cluster in minutes with good practices in it and without manual work.
