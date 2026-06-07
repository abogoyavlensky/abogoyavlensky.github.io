Codign agents are great, but running with all permissions enabled is even better.
So I've been searching a good approach on conveninent and reliable isolation for sandbxoing them.

### First attempts

I've tried few things so far. Probably the most obvious idea was Docker Containers.
This great, liteweight and useful solution, and it still fine to use, but 
when you want to run docker-in-docker for examlpe for running tests with a database in a container using testcontaers for examlpe
it's not that safe, because docker-in-docker requires host socket access form inside the container. That basically gives 
uncontrollable priviledageds to an angent. Also the container boundary is thin: shared kernel, syscall exposure, escapre CVEs.

So I kept searching, and tried Docker Sandboxes. It's might not be the most obvios chois as the project is quite new and still experimental.
However it was on my radar so I tried, and it worked quite well. The idea is to spin up a new microVM on each project.
It has few default templates per coding agent, or you can configure a custom one. So, it's already a rel VM isolation. Docker inside VM is available out of the box wihout 
restrictions or shortcuts. Relatively easy to spinup.

However, there are few downsides that I found quite annoying:
- a VM is quite heavy to create, and requires at least 10 - 15 GB disk space.
- inflecible - mounts frozen at creation, you can't change mounted dirs once VM is created. Also no configurable mount point iside the VM for shared dirs. 
- limited env vars - only few approved env vars for agent login, everything else you have to copy-paste manually in terminal
- HTTP proxy - even dependency fetching is quite painful

Verdict: right isolation level, but too heavy and too inflexible for daily use.

### Searching alternatives

Then I tried to look at all alternatives exist at this point. I split them in two groups:
1. VMs and microMVs:
  - Vagrant - classic VM orchestrator, quite heavy and not flexible this days.
  - Lima - Ligthweight Linus VMs
  - Apple COntainer - Apple's native container runtime, it looks quite promising, but docker-in-docker is not straighforward (if even possible), and it's more common with Docker Containers in usability.
  - Lume/Tart/vfkit - probably too lower-level Apple Virtualization.Framework wrappers
  - vibe/vibebox/Shuru/... - modern attempts to solve agent soandboxing with microVMs or Docker Containers - right direction, but maybe at early days

2. Non-VM sandboxing:
  - sandbox-exec - macOS syscal filter
  - bubblewrap - Linux namespace sandbox (it's included by default to Claude Code and Codex)
  - Agent Safehouse - wrapper for macos sanfbox-exec with composable policy profiles
  - Zerobox/... - other alternatives and different combination of configurable namespace isolation.

### My go-to solution: Lima

I ended up with the idea that I'm confortable with maximum lavel of isolation with VM or microVMs. So tried few other alternatives from this category, and ended up 
with choosing Lima as my go-to solution for agent sandboxing during local development.

Lima is a lightweigth Linux VMs. On macOS it uses Apple's Virtualization.Framework. Same isolation model as Docker Sandboxes, without the pain.

What I like about user experience with Lima:
- Fast to create a VM and reasonable defaults.
- Mounts can be changed after Vm creation.
- Custom mount point inside a VM is possible. (useful for sharing skills for example)
- Disk grows on demand
- Full controll over env vars
- Scriptable setup
- by defualt it exposes servers running on localhost to the host system - so manual testing of web apps is trivial

Some minor cons:
- Mount point insde a VM is configurared via the yaml file only - no way to set it up using only cli tool.
- Egress control is stil unsolved - you have to figure out how to retrict outgoing HTTP request on your own: local proxies, `/etc/hosts`, anything else.

### My Lima setup

Conceptually I ended up with creating one VM for all my projects that live in a single dir on my host machine at `/path/to/Projcts`.
I tried a VM per project, but it wasn't convenient, because sometimes I want to add some more dirs to context. Also, I don't see the value in such gramular isolation as soon as we have git worktrees for
parallel work on different features in a same projects. 

So I have one VM for develpment where I mount all my projects and skills dir. I intentionally don't share full config dirs for agents such as `~/.claude` or `~/.codex` and keep them localy inside the VM. 

I've created my own custom [startup script](https://github.com/abogoyavlensky/agents/blob/master/sandbox/agent.yaml) that includes few useful system deps, [mise](https://mise.jdx.dev/) - so we can install any tools conveniently inside the VM,
homebrew for linus - for the same reason, few popular coding agents and some shell aliases. 

You can start from scratch from any official tempalte that Lima provides, make your custom one, or try my template from above.

Also I have few convenient [shell functions](https://github.com/abogoyavlensky/agents/blob/master/sandbox/README.md?plain=1#L59-L86) that simplifies the usage of agents via sandboxed VMs. So the idea is to make VM invisible, currently there is no UX difference form to run `claude` or say `lmcc`
- it's the same speed, same convenients, but completely different level of isolation.

### Workflow

Now let's take a look at my typical workflow of usage an agent with Lima.

Let's first install Lima on your host machine:

```bash
brew install lima
```

For simplest setup, you can use use one of the built-in templates:

```bash
limactl create --name sandbox
```

`--name` can be anything you want.

Alternatively, to have a little bit more out of the box you can use my startup script:

```bash
<curl to fetch my config file https://github.com/abogoyavlensky/agents/blob/master/sandbox/agent.yaml>
limactl create --name sandbox ./agent.yaml
```

Once VM is created we can mount our projects dir, let's `cd` to it and start the VM  with edit mode:

```bash
cd ~/Projects   # or any directory you want to expose
limactl edit sandbox --mount-only .:w --start
```

You can add more dirs after you stop the VM and extend `mounts` section in automatically opened yaml file on `limactl edit`:

```bash
limactl stop sandbox
limactl edit sandbox
```

Then, if your skills directory is outside of the `Projects` directory you mounted above, 
add 

```bash
mounts:
...
- location: "/Users/andrew/Projects/agents/skills"
  mountPoint: "/home/agent.guest/.claude/skills"
  writable: true
- location: "/Users/andrew/Projects/agents/skills"
  mountPoint: "/home/agent.guest/.agents/skills"
  writable: true
```

OR if you have your skills dir inside the `Projects` directory, you can just 
link it to the right place inside the VM:

```bash
ln -s /workspace/Projects/agents/skills ~/.claude/skills
ln -s /workspace/Projects/agents/skills ~/.agents/skills
```

Ok, now you can start a VM in regular shell mode from curretn dir on the host if it's inside of one of th mounted dirs:




#### Aliases

```bash
LIMA_DEFAULT_VM="sandbox"

lm() {
  LIMA_SHELLENV_BLOCK=* LIMA_SHELLENV_ALLOW=GH_TOKEN limactl shell --preserve-env $LIMA_DEFAULT_VM -- "$@"
}

# Open a shell in the VM with GH_TOKEN forwarded: `lmsh`
lmsh() {
  LIMA_SHELLENV_BLOCK=* LIMA_SHELLENV_ALLOW=GH_TOKEN limactl shell --preserve-env $LIMA_DEFAULT_VM
}

lmcc() {
  lm claude --dangerously-skip-permissions "$@"
}

lmcx() {
  lm codex --yolo "$@"
}

lmcop() {
  lm copilot --yolo "$@"
}

lmkiro() {
  lm kiro-cli chat --trust-all-tools "$@"
}
```



### Takeways

- The outgoing HTTP requests would be great to whilte list somehow, or atleast have abilty to control. Currently I'm using `/etc/hosts` approach to restrict some domains: `127.0.0.1 some.domain.com`
- Some tools for orchestration of agents do not work with VMs, but mayeb if you will share config dirs of agents from host to VM, you will probably can use more such tools.
- I actually found myself that I'm running more and more inside the VM even not only things related to agents: actually eny other thing I'm running/installing inside the vm and keep my host machine clean.
