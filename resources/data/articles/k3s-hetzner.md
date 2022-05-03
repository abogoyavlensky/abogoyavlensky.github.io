This is just a quick note about provisioning self-hosted Kubernetes cluster in Hetzner.
For large production I would sugest to use any managed Kubernets from AWS, Google Cloud or DigitalOcean you name it.
But sometimes you need some small cluster for personal projects or medium apps. 
And for that case managed solutions could be a bit expensive. 

So recently I faced with this kind of problem and decided to figure out a way how to 
bootstrap self-hosted Kubernetes cluster but do not spend a lot of time 
to dealing with countless Kubernetes internal parts. 
Also I would like to use Terraform as much as possible to avoid manual configuration of the cluster.
I would like to use dynamically attached volumes to pods using CSI or similar. 
In other words, the less support cluster would need in the future the better.
For a hosting I choose [Hetzner](https://www.hetzner.com/cloud) as a probably cheapest and one of the most roubust one.

Finally I end up with combination: [k3s](https://k3s.io/) + [kube-hetzner](https://github.com/kube-hetzner/terraform-hcloud-kube-hetzner/blob/master/terraform.tfvars.example) Terraform module.
And in this note I would like to share my setup in [github repo](https://github.com/abogoyavlensky/k3s-provision).
All you need for installing a new cluster on fresh Hetzner project space is:
- clone the repo: `git clone git@github.com:abogoyavlensky/k3s-provision.git`;
- create the file [`env.auto.tfvars`](https://github.com/abogoyavlensky/k3s-provision/blob/master/env.auto.tfvars.example) at the root of repo directory with two vars `hcloud_token` and `traefik_acme_email`; (*This file must not be commited!*)
- and run `terraform apply`.

After confirmation terraform starts to install cluster which contains:
- one load-balancer node;
- one control-plane node;
- one worker node.

*It costs about ~16 Euro.*

The kube-hetzner module allows us to install trully HA cluster but for personal projects I dont need that so I decided to reduce the cost.
Please check config in the [main terraform file](https://github.com/abogoyavlensky/k3s-provision/blob/master/main.tf) in the repo.
Every possible config and some doc you could find in [the official example](https://github.com/kube-hetzner/terraform-hcloud-kube-hetzner/blob/master/terraform.tfvars.example). There is ability to increase worker and control plane nodes on the go. 
I disabled autoupgrade Kubernetes, but it is possible to upgrade it with a little bit mannula work. 
I have a plan to figure it out when it will be needed.

For demonstration purposes of cluster abilities there is [an example](https://github.com/abogoyavlensky/k3s-provision/blob/master/examples/nginx.yaml) nginx service with dynamically attached volume.
But before deploying the service let's install traefik middleware for redirecting http requests to https:

```shell
kubectl apply -f deploy/middlewares.yaml
```

And edit [host](https://github.com/abogoyavlensky/k3s-provision/blob/3fcbb9a6943b48dedb347c4f47e3f1af78e72b80/examples/nginx.yaml#L61) with your domain in kubernetes yaml to check the actual service with https.

Then you could deploy the example service:

```shell
kubectl apply -f example/nginx.yaml
```

After few seconds you could check the `your.domain.com` and see the nginx greeting page.

That's how I finally ran my Kubernetes cluster for personal projects.
Of course there is a room for improvements:
- attaching floating to load-balancer;
- using terraform tfstate backend for not storing infra state locally;
- increasing controll planes at least up to 3 and worker nodes as needed;
- checking terraform files formatting and correctness in CI on every commit.
